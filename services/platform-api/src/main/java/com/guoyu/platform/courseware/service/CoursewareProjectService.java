package com.guoyu.platform.courseware.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.courseware.domain.CoursewareModels.DigestDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.SourceSection;
import com.guoyu.platform.courseware.entity.CwOutlinePageEntity;
import com.guoyu.platform.courseware.entity.CwProjectEntity;
import com.guoyu.platform.courseware.mapper.CwOutlinePageMapper;
import com.guoyu.platform.courseware.mapper.CwProjectMapper;
import com.guoyu.platform.courseware.mapper.CwSourceSectionMapper;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * 课件项目编排(方案 §4/§11):上传 → 异步流水线(解析→消化→大纲)。
 * 租户/操作人 HTTP 线程捕获后参数传递;Worker 线程自行 set/clear TenantContextHolder。
 */
@Slf4j
@Service
public class CoursewareProjectService {

  private final CwProjectMapper projectMapper;
  private final CwSourceSectionMapper sectionMapper;
  private final CwOutlinePageMapper outlineMapper;
  private final DocxSectionParser docxParser;
  private final CoursewareDigestService digestService;
  private final CoursewareOutlineService outlineService;
  private final FileApplicationService fileService;
  private final ThreadPoolTaskExecutor coursewareExecutor;

  public CoursewareProjectService(
      CwProjectMapper projectMapper,
      CwSourceSectionMapper sectionMapper,
      CwOutlinePageMapper outlineMapper,
      DocxSectionParser docxParser,
      CoursewareDigestService digestService,
      CoursewareOutlineService outlineService,
      FileApplicationService fileService,
      @Qualifier("coursewareExecutor") ThreadPoolTaskExecutor coursewareExecutor) {
    this.projectMapper = projectMapper;
    this.sectionMapper = sectionMapper;
    this.outlineMapper = outlineMapper;
    this.docxParser = docxParser;
    this.digestService = digestService;
    this.outlineService = outlineService;
    this.fileService = fileService;
    this.coursewareExecutor = coursewareExecutor;
  }

  // ──────────────── HTTP 线程 ────────────────

  /** 创建项目并派发异步流水线(秒回;踩坑 #11) */
  public CwProjectEntity createProject(Long tenantId, Long userId, Long sourceFileId,
                                       String title, String audience, String contentDensity) {
    FileEntity file = fileService.getExistingFile(tenantId, sourceFileId);
    String name = file.getOriginalName() == null ? "" : file.getOriginalName();
    if (!name.toLowerCase().endsWith(".docx")) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只支持 .docx 格式的标准文档");
    }
    CwProjectEntity project = new CwProjectEntity();
    project.setId(IdWorker.getId());
    project.setTenantId(tenantId);
    project.setTitle(title == null || title.isBlank()
        ? name.replaceAll("\\.docx$", "") : title.trim());
    project.setSourceFileId(sourceFileId);
    project.setAudience(audience == null || audience.isBlank() ? "学员自学" : audience.trim());
    project.setContentDensity(
        "concise".equals(contentDensity) || "detailed".equals(contentDensity) ? contentDensity
            : "medium");
    project.setThemeId("enterprise");
    project.setStatus(CwProjectEntity.STATUS_DRAFT);
    project.setTotalPages(0);
    project.setReadyPages(0);
    project.setFailedPages(0);
    project.setCreatedBy(userId);
    project.setUpdatedBy(userId);
    projectMapper.insert(project);
    try {
      coursewareExecutor.submit(() -> runPipeline(project.getId(), tenantId, userId));
    } catch (RejectedExecutionException e) {
      markFailed(project.getId(), "服务繁忙,请稍后重试");
      throw e; // 全局异常处理返回 503(坑 #12)
    }
    return project;
  }

  public PageResponse<CwProjectEntity> pageProjects(Long tenantId, long page, long pageSize) {
    Page<CwProjectEntity> result = projectMapper.selectPage(new Page<>(page, pageSize),
        new LambdaQueryWrapper<CwProjectEntity>()
            .eq(CwProjectEntity::getTenantId, tenantId)
            .orderByDesc(CwProjectEntity::getId));
    return new PageResponse<>(result.getRecords(), result.getCurrent(), result.getSize(),
        result.getTotal());
  }

  public CwProjectEntity getProject(Long tenantId, Long projectId) {
    CwProjectEntity project = projectMapper.selectById(projectId);
    if (project == null || !tenantId.equals(project.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "课件项目不存在");
    }
    return project;
  }

  public void deleteProject(Long tenantId, Long projectId) {
    CwProjectEntity project = getProject(tenantId, projectId);
    if (CwProjectEntity.STATUS_GENERATING.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "生成中的项目不能删除,请先取消");
    }
    projectMapper.deleteById(projectId);
  }

  // ──────────────── Worker 线程:解析 → 消化 → 大纲 ────────────────

  void runPipeline(Long projectId, Long tenantId, Long userId) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      CwProjectEntity project = projectMapper.selectById(projectId);
      if (project == null) {
        return;
      }
      updateStatus(projectId, CwProjectEntity.STATUS_PARSING);

      // 1) 解析 docx
      List<SourceSection> parsed;
      try (InputStream in = fileService.openFileInputStreamById(project.getSourceFileId())) {
        parsed = docxParser.parse(in.readAllBytes());
      }
      if (parsed.isEmpty()) {
        markFailed(projectId, "这份文档没有识别出可用内容,请确认是文字版 docx");
        return;
      }
      // 赋稳定引用号 S1:N 并落库
      for (int i = 0; i < parsed.size(); i++) {
        SourceSection s = parsed.get(i);
        var entity = new com.guoyu.platform.courseware.entity.CwSourceSectionEntity();
        entity.setId(IdWorker.getId());
        entity.setTenantId(tenantId);
        entity.setProjectId(projectId);
        entity.setRef("S1:" + (i + 1));
        entity.setHeading(s.heading());
        entity.setLevel(s.level());
        entity.setText(s.text());
        entity.setLocator(s.locator());
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        sectionMapper.insert(entity);
        parsed.set(i, new SourceSection(entity.getRef(), s.heading(), s.level(), s.text(),
            s.locator()));
      }

      // 2) 消化层(失败降级不阻断)
      updateStatus(projectId, CwProjectEntity.STATUS_DIGESTING);
      DigestDraft digest = digestService.tryDigest(tenantId, projectId, userId, project.getTitle(),
          project.getAudience(), parsed);

      // 3) 大纲生成
      updateStatus(projectId, CwProjectEntity.STATUS_OUTLINE_GENERATING);
      List<CwOutlinePageEntity> pages = outlineService.generateOutlinePages(tenantId, projectId,
          userId, project.getTitle(), project.getAudience(), project.getContentDensity(), parsed,
          digest);
      for (CwOutlinePageEntity page : pages) {
        outlineMapper.insert(page);
      }
      updateStatus(projectId, CwProjectEntity.STATUS_OUTLINE_READY);
      log.info("课件大纲就绪: projectId={}, pages={}", projectId, pages.size());
    } catch (BusinessException e) {
      markFailed(projectId, e.getMessage());
    } catch (Exception e) {
      log.error("课件项目流水线异常: projectId={}", projectId, e);
      markFailed(projectId, "处理失败,请重试");
    } finally {
      TenantContextHolder.clear();
    }
  }

  private void updateStatus(Long projectId, String status) {
    CwProjectEntity update = new CwProjectEntity();
    update.setId(projectId);
    update.setStatus(status);
    projectMapper.updateById(update);
  }

  private void markFailed(Long projectId, String message) {
    CwProjectEntity update = new CwProjectEntity();
    update.setId(projectId);
    update.setStatus(CwProjectEntity.STATUS_FAILED);
    update.setError(message == null ? "处理失败,请重试"
        : message.length() > 480 ? message.substring(0, 480) : message);
    projectMapper.updateById(update);
  }
}
