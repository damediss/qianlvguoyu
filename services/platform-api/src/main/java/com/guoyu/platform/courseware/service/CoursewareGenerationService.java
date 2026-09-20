package com.guoyu.platform.courseware.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.courseware.domain.CoursewareLayout;
import com.guoyu.platform.courseware.domain.CoursewareModels.DigestDraft;
import com.guoyu.platform.courseware.domain.CoursewareModels.DigestTopic;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideGenInput;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideWorkflowResult;
import com.guoyu.platform.courseware.domain.CoursewareModels.SourceSection;
import com.guoyu.platform.courseware.entity.CwDigestEntity;
import com.guoyu.platform.courseware.entity.CwOutlinePageEntity;
import com.guoyu.platform.courseware.entity.CwProjectEntity;
import com.guoyu.platform.courseware.entity.CwSlideEntity;
import com.guoyu.platform.courseware.entity.CwSourceSectionEntity;
import com.guoyu.platform.courseware.mapper.CwDigestMapper;
import com.guoyu.platform.courseware.mapper.CwOutlinePageMapper;
import com.guoyu.platform.courseware.mapper.CwProjectMapper;
import com.guoyu.platform.courseware.mapper.CwSlideMapper;
import com.guoyu.platform.courseware.mapper.CwSourceSectionMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * 页级生成编排(方案 §11):确认大纲后派发页任务到 coursewareSlideExecutor;
 * 单页失败隔离可重试;软取消只挡新派发,正在跑的页跑完。
 */
@Slf4j
@Service
public class CoursewareGenerationService {

  /** 单页来源裁剪(方案 §8.2):单节 1500、合计 6000 */
  static final int SLIDE_SECTION_CHARS = 1_500;
  static final int SLIDE_TOTAL_CHARS = 6_000;

  private final CwProjectMapper projectMapper;
  private final CwSlideMapper slideMapper;
  private final CwOutlinePageMapper outlineMapper;
  private final CwSourceSectionMapper sectionMapper;
  private final CwDigestMapper digestMapper;
  private final SlideWorkflowService workflowService;
  private final ObjectMapper objectMapper;
  private final ThreadPoolTaskExecutor coursewareExecutor;
  private final ThreadPoolTaskExecutor slideExecutor;

  /** 软取消标记(projectId);编排线程每派发一页前检查 */
  private final Map<Long, Boolean> cancelFlags = new ConcurrentHashMap<>();

  public CoursewareGenerationService(
      CwProjectMapper projectMapper,
      CwSlideMapper slideMapper,
      CwOutlinePageMapper outlineMapper,
      CwSourceSectionMapper sectionMapper,
      CwDigestMapper digestMapper,
      SlideWorkflowService workflowService,
      ObjectMapper objectMapper,
      @Qualifier("coursewareExecutor") ThreadPoolTaskExecutor coursewareExecutor,
      @Qualifier("coursewareSlideExecutor") ThreadPoolTaskExecutor slideExecutor) {
    this.projectMapper = projectMapper;
    this.slideMapper = slideMapper;
    this.outlineMapper = outlineMapper;
    this.sectionMapper = sectionMapper;
    this.digestMapper = digestMapper;
    this.workflowService = workflowService;
    this.objectMapper = objectMapper;
    this.coursewareExecutor = coursewareExecutor;
    this.slideExecutor = slideExecutor;
  }

  // ──────────────── HTTP 线程 ────────────────

  /** 确认大纲后派发页级生成(秒回;拒绝 → 项目回退 outline_ready 并抛 503) */
  public void dispatchGeneration(Long tenantId, Long userId, Long projectId) {
    try {
      coursewareExecutor.submit(() -> runGeneration(projectId, tenantId, userId));
    } catch (RejectedExecutionException e) {
      CwProjectEntity rollback = new CwProjectEntity();
      rollback.setId(projectId);
      rollback.setStatus(CwProjectEntity.STATUS_OUTLINE_READY);
      rollback.setError("服务繁忙,请稍后重试");
      projectMapper.updateById(rollback);
      throw e;
    }
  }

  /** 失败页/残留问题页单独重试(failed 或 ready 带问题可重试;导出门禁阻断后的修复入口) */
  public void retrySlide(Long tenantId, Long userId, Long slideId) {
    CwSlideEntity slide = slideMapper.selectById(slideId);
    if (slide == null || !tenantId.equals(slide.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "页面不存在");
    }
    if (!CwSlideEntity.STATUS_FAILED.equals(slide.getStatus())
        && !CwSlideEntity.STATUS_READY.equals(slide.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "生成中或排队中的页面不能重试");
    }
    try {
      slideExecutor.submit(() -> generateOne(slide.getProjectId(), slideId, tenantId, userId));
    } catch (RejectedExecutionException e) {
      throw e; // 全局 503
    }
  }

  /** 软取消:只挡新派发(方案 §11) */
  public void cancel(Long tenantId, Long projectId) {
    CwProjectEntity project = projectMapper.selectById(projectId);
    if (project == null || !tenantId.equals(project.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "课件项目不存在");
    }
    if (!CwProjectEntity.STATUS_GENERATING.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅生成中的项目可以取消");
    }
    cancelFlags.put(projectId, Boolean.TRUE);
  }

  // ──────────────── Worker 线程 ────────────────

  void runGeneration(Long projectId, Long tenantId, Long userId) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      List<CwSlideEntity> slides = slideMapper.selectList(new LambdaQueryWrapper<CwSlideEntity>()
          .eq(CwSlideEntity::getTenantId, tenantId)
          .eq(CwSlideEntity::getProjectId, projectId)
          .in(CwSlideEntity::getStatus, CwSlideEntity.STATUS_PENDING, CwSlideEntity.STATUS_FAILED)
          .orderByAsc(CwSlideEntity::getPosition));
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      boolean cancelled = false;
      for (CwSlideEntity slide : slides) {
        // 软取消只挡新派发:正在跑的页跑完(方案 §11);标志在 finally 统一清理
        if (cancelFlags.containsKey(projectId)) {
          cancelled = true;
          break;
        }
        futures.add(CompletableFuture.runAsync(
            () -> generateOne(projectId, slide.getId(), tenantId, userId), slideExecutor));
      }
      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
      finalizeProject(projectId, cancelled);
    } catch (Exception e) {
      log.error("课件生成编排异常: projectId={}", projectId, e);
      CwProjectEntity update = new CwProjectEntity();
      update.setId(projectId);
      update.setStatus(CwProjectEntity.STATUS_PARTIAL);
      update.setError("生成过程异常,请重试失败页面");
      projectMapper.updateById(update);
    } finally {
      cancelFlags.remove(projectId);
      TenantContextHolder.clear();
    }
  }

  /** 单页生成(Worker 线程;事务外;失败隔离) */
  void generateOne(Long projectId, Long slideId, Long tenantId, Long userId) {
    TenantContextHolder.setTenantId(tenantId);
    try {
      if (slideMapper.markGenerating(slideId) == 0) {
        return; // 已被其它派发占用或状态非法
      }
      CwSlideEntity slide = slideMapper.selectById(slideId);
      CwProjectEntity project = projectMapper.selectById(projectId);
      if (slide == null || project == null) {
        return;
      }
      SlideGenInput input = buildInput(project, slide);
      SlideWorkflowResult result = workflowService.execute(input, tenantId, projectId, userId,
          slideId);
      CwSlideEntity update = new CwSlideEntity();
      update.setId(slideId);
      update.setBlocks(objectMapper.writeValueAsString(result.blocks()));
      update.setSpeakerNotes(result.speakerNotes());
      update.setIssues(objectMapper.writeValueAsString(result.issues()));
      update.setStatus(CwSlideEntity.STATUS_READY);
      update.setError(null);
      update.setRevision(slide.getRevision() + 1);
      slideMapper.updateById(update);
      projectMapper.incrementReady(projectId);
      refreshProjectStatusIfIdle(projectId);
    } catch (Exception e) {
      log.warn("课件单页生成失败: slideId={}, error={}", slideId, e.getMessage());
      CwSlideEntity update = new CwSlideEntity();
      update.setId(slideId);
      update.setStatus(CwSlideEntity.STATUS_FAILED);
      update.setError("页面生成失败,请重试");
      slideMapper.updateById(update);
      projectMapper.incrementFailed(projectId);
      refreshProjectStatusIfIdle(projectId);
    } finally {
      TenantContextHolder.clear();
    }
  }

  /** 单页重试后项目状态重算(编排已结束时):按 slides 实况刷新计数与状态 */
  private void refreshProjectStatusIfIdle(Long projectId) {
    CwProjectEntity project = projectMapper.selectById(projectId);
    if (project == null || CwProjectEntity.STATUS_GENERATING.equals(project.getStatus())) {
      return; // 编排进行中,由 finalizeProject 统一收口
    }
    List<CwSlideEntity> slides = slideMapper.selectList(new LambdaQueryWrapper<CwSlideEntity>()
        .eq(CwSlideEntity::getProjectId, projectId));
    long ready = slides.stream().filter(s -> CwSlideEntity.STATUS_READY.equals(s.getStatus()))
        .count();
    long failed = slides.stream().filter(s -> CwSlideEntity.STATUS_FAILED.equals(s.getStatus()))
        .count();
    long generating = slides.stream()
        .filter(s -> CwSlideEntity.STATUS_GENERATING.equals(s.getStatus())).count();
    if (generating > 0) {
      return;
    }
    CwProjectEntity update = new CwProjectEntity();
    update.setId(projectId);
    update.setReadyPages((int) ready);
    update.setFailedPages((int) failed);
    if (!CwProjectEntity.STATUS_CANCELLED.equals(project.getStatus())) {
      update.setStatus(failed > 0 ? CwProjectEntity.STATUS_PARTIAL : CwProjectEntity.STATUS_READY);
      update.setError(failed > 0 ? "部分页面生成失败,可在页列表单独重试" : null);
    }
    projectMapper.updateById(update);
  }

  /** 组装单页独立上下文(方案 §8.2):本页小节 + digest 片段 + 相邻页标题 */
  SlideGenInput buildInput(CwProjectEntity project, CwSlideEntity slide) {
    CwOutlinePageEntity page = outlineMapper.selectById(slide.getOutlinePageId());
    if (page == null) {
      throw new IllegalStateException("大纲页不存在: " + slide.getOutlinePageId());
    }
    Map<String, SourceSection> sectionsByRef = loadSections(project.getId());
    List<String> refs = readStringList(page.getSourceRefs());
    List<SourceSection> pageSections = new ArrayList<>();
    for (String ref : refs) {
      SourceSection section = sectionsByRef.get(ref);
      if (section != null) {
        pageSections.add(section);
      }
    }
    pageSections = CoursewareDigestService.trimSections(pageSections, SLIDE_SECTION_CHARS,
        SLIDE_TOTAL_CHARS);
    String digestSlice = digestSlice(project.getId(), refs);
    List<String> neighbors = neighborTitles(project.getId(), page.getPosition());
    return new SlideGenInput(
        project.getTitle(),
        project.getAudience(),
        page.getPosition(),
        project.getTotalPages(),
        page.getChapterTitle(),
        page.getTitle(),
        page.getObjective(),
        readStringList(page.getKeyPoints()),
        page.getPageRole(),
        slide.getLayoutId() != null ? slide.getLayoutId()
            : CoursewareLayout.layoutForRole(page.getPageRole(), null),
        pageSections,
        digestSlice,
        neighbors,
        project.getContentDensity(),
        List.of());
  }

  private Map<String, SourceSection> loadSections(Long projectId) {
    List<CwSourceSectionEntity> entities = sectionMapper.selectList(
        new LambdaQueryWrapper<CwSourceSectionEntity>()
            .eq(CwSourceSectionEntity::getProjectId, projectId));
    return entities.stream().collect(Collectors.toMap(CwSourceSectionEntity::getRef,
        e -> new SourceSection(e.getRef(), e.getHeading(), e.getLevel(), e.getText(),
            e.getLocator())));
  }

  /** 该页 refs 命中的 digest topic 片段(无 digest 时返回 null 降级) */
  private String digestSlice(Long projectId, List<String> refs) {
    List<CwDigestEntity> digests = digestMapper.selectList(new LambdaQueryWrapper<CwDigestEntity>()
        .eq(CwDigestEntity::getProjectId, projectId)
        .eq(CwDigestEntity::getStatus, CwDigestEntity.STATUS_READY)
        .last("LIMIT 1"));
    if (digests.isEmpty() || digests.get(0).getTopics() == null) {
      return null;
    }
    try {
      JsonNode topicsNode = objectMapper.readTree(digests.get(0).getTopics());
      StringBuilder sb = new StringBuilder();
      for (JsonNode node : topicsNode) {
        boolean hit = false;
        for (JsonNode ref : node.path("source_refs")) {
          if (refs.contains(ref.asText())) {
            hit = true;
            break;
          }
        }
        if (!hit) {
          continue;
        }
        sb.append("【").append(node.path("name").asText()).append("】\n");
        node.path("points").forEach(p -> sb.append("- ").append(p.asText()).append('\n'));
        JsonNode confusions = node.path("confusions");
        if (confusions.isArray() && !confusions.isEmpty()) {
          sb.append("易错提醒:\n");
          confusions.forEach(c -> sb.append("- ").append(c.asText()).append('\n'));
        }
        JsonNode keyData = node.path("key_data");
        if (keyData.isArray() && !keyData.isEmpty()) {
          sb.append("关键数据:\n");
          keyData.forEach(d -> sb.append("- ").append(d.asText()).append('\n'));
        }
      }
      String slice = sb.toString().trim();
      return slice.isEmpty() ? null : slice;
    } catch (Exception e) {
      log.warn("digest 片段解析失败(降级不用): projectId={}, error={}", projectId, e.getMessage());
      return null;
    }
  }

  private List<String> neighborTitles(Long projectId, int position) {
    List<CwOutlinePageEntity> pages = outlineMapper.selectList(
        new LambdaQueryWrapper<CwOutlinePageEntity>()
            .eq(CwOutlinePageEntity::getProjectId, projectId)
            .orderByAsc(CwOutlinePageEntity::getPosition));
    List<String> titles = new ArrayList<>();
    int start = Math.max(0, position - 3);
    int end = Math.min(pages.size(), position + 2);
    for (int i = start; i < end; i++) {
      if (pages.get(i).getPosition() != position) {
        titles.add(pages.get(i).getTitle());
      }
    }
    return titles;
  }

  private void finalizeProject(Long projectId, boolean cancelled) {
    CwProjectEntity project = projectMapper.selectById(projectId);
    if (project == null) {
      return;
    }
    CwProjectEntity update = new CwProjectEntity();
    update.setId(projectId);
    if (cancelled) {
      // 未派发的 pending 页落 cancelled(方案 §11)
      List<CwSlideEntity> pending = slideMapper.selectList(new LambdaQueryWrapper<CwSlideEntity>()
          .eq(CwSlideEntity::getProjectId, projectId)
          .eq(CwSlideEntity::getStatus, CwSlideEntity.STATUS_PENDING));
      for (CwSlideEntity slide : pending) {
        CwSlideEntity u = new CwSlideEntity();
        u.setId(slide.getId());
        u.setStatus(CwSlideEntity.STATUS_CANCELLED);
        slideMapper.updateById(u);
      }
      update.setStatus(CwProjectEntity.STATUS_CANCELLED);
      update.setError("生成已取消,已完成的页面保留");
    } else if (project.getFailedPages() != null && project.getFailedPages() > 0) {
      update.setStatus(CwProjectEntity.STATUS_PARTIAL);
      update.setError("部分页面生成失败,可在页列表单独重试");
    } else {
      update.setStatus(CwProjectEntity.STATUS_READY);
      update.setError(null);
    }
    projectMapper.updateById(update);
  }

  private List<String> readStringList(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<String> out = new ArrayList<>();
      for (JsonNode node : objectMapper.readTree(json)) {
        if (node.isTextual()) {
          out.add(node.asText());
        }
      }
      return out;
    } catch (Exception e) {
      return List.of();
    }
  }
}
