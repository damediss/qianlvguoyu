package com.guoyu.platform.courseware.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.courseware.domain.CoursewareLayout;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideBlock;
import com.guoyu.platform.courseware.domain.SlideValidator;
import com.guoyu.platform.courseware.domain.StructureIssue;
import com.guoyu.platform.courseware.entity.CwOutlinePageEntity;
import com.guoyu.platform.courseware.entity.CwProjectEntity;
import com.guoyu.platform.courseware.entity.CwSlideEntity;
import com.guoyu.platform.courseware.entity.CwSourceSectionEntity;
import com.guoyu.platform.courseware.mapper.CwOutlinePageMapper;
import com.guoyu.platform.courseware.mapper.CwProjectMapper;
import com.guoyu.platform.courseware.mapper.CwSlideMapper;
import com.guoyu.platform.courseware.mapper.CwSourceSectionMapper;
import com.guoyu.platform.courseware.service.PptxRenderer.RenderSlide;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.file.vo.FileVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 导出服务(方案 §10):导出前门禁(error 阻断/warning 透出) → 渲染 → 回读验证 → 落 file 包。
 * 页脚来源标注用 source_refs→locator 确定性映射,不依赖模型自觉(口径 13)。
 */
@Slf4j
@Service
public class CoursewareExportService {

  private final CwProjectMapper projectMapper;
  private final CwSlideMapper slideMapper;
  private final CwOutlinePageMapper outlineMapper;
  private final CwSourceSectionMapper sectionMapper;
  private final FileApplicationService fileService;
  private final ObjectMapper objectMapper;

  public CoursewareExportService(CwProjectMapper projectMapper, CwSlideMapper slideMapper,
                                 CwOutlinePageMapper outlineMapper,
                                 CwSourceSectionMapper sectionMapper,
                                 FileApplicationService fileService, ObjectMapper objectMapper) {
    this.projectMapper = projectMapper;
    this.slideMapper = slideMapper;
    this.outlineMapper = outlineMapper;
    this.sectionMapper = sectionMapper;
    this.fileService = fileService;
    this.objectMapper = objectMapper;
  }

  /** 导出结果:blocked=true 时 issues 为阻断清单;否则 fileId + warnings 透出 */
  public record ExportResult(boolean blocked, List<StructureIssue> issues, Long fileId,
                             List<StructureIssue> warnings) {
  }

  public ExportResult export(Long tenantId, Long userId, Long projectId) {
    CwProjectEntity project = projectMapper.selectById(projectId);
    if (project == null || !tenantId.equals(project.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "课件项目不存在");
    }
    if (!CwProjectEntity.STATUS_READY.equals(project.getStatus())
        && !CwProjectEntity.STATUS_PARTIAL.equals(project.getStatus())
        && !CwProjectEntity.STATUS_CANCELLED.equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "课件尚未生成完成,暂不能导出");
    }

    List<CwSlideEntity> slides = slideMapper.selectList(new LambdaQueryWrapper<CwSlideEntity>()
        .eq(CwSlideEntity::getTenantId, tenantId)
        .eq(CwSlideEntity::getProjectId, projectId)
        .eq(CwSlideEntity::getStatus, CwSlideEntity.STATUS_READY)
        .orderByAsc(CwSlideEntity::getPosition));
    if (slides.isEmpty()) {
      throw new BusinessException(ErrorCode.CONFLICT, "没有已生成的页面,暂不能导出");
    }

    Map<Long, CwOutlinePageEntity> outlineById = outlineMapper.selectList(
            new LambdaQueryWrapper<CwOutlinePageEntity>()
                .eq(CwOutlinePageEntity::getProjectId, projectId))
        .stream().collect(LinkedHashMap::new, (m, p) -> m.put(p.getId(), p), Map::putAll);
    Map<String, CwSourceSectionEntity> sectionsByRef = sectionMapper.selectList(
            new LambdaQueryWrapper<CwSourceSectionEntity>()
                .eq(CwSourceSectionEntity::getProjectId, projectId))
        .stream().collect(LinkedHashMap::new, (m, s) -> m.put(s.getRef(), s), Map::putAll);

    // 1) 导出前门禁:重跑校验,error 阻断(方案 §10.5)
    List<StructureIssue> errors = new ArrayList<>();
    List<StructureIssue> warnings = new ArrayList<>();
    Map<Long, List<SlideBlock>> blocksBySlide = new LinkedHashMap<>();
    Map<Long, List<StructureIssue>> issuesBySlide = new LinkedHashMap<>();
    for (CwSlideEntity slide : slides) {
      CwOutlinePageEntity page = outlineById.get(slide.getOutlinePageId());
      String pageRole = page != null ? page.getPageRole() : "content";
      String layoutId = slide.getLayoutId() != null ? slide.getLayoutId()
          : CoursewareLayout.layoutForRole(pageRole, null);
      CoursewareLayout.Layout layout = CoursewareLayout.require(layoutId);
      List<SlideBlock> blocks = parseBlocks(slide.getBlocks());
      // 导出前门禁:全量校验(含 structure error),方案 §10.5
      List<StructureIssue> issues = new ArrayList<>(
          SlideValidator.validate(blocks, layout, pageRole, project.getContentDensity()));
      issues.forEach(i -> (i.isError() ? errors : warnings).add(withPosition(i, slide)));
      blocksBySlide.put(slide.getId(), blocks);
      issuesBySlide.put(slide.getId(), issues);
    }
    if (!errors.isEmpty()) {
      return new ExportResult(true, errors, null, warnings);
    }

    // 2) 渲染(页脚来源确定性标注)
    List<RenderSlide> renderSlides = new ArrayList<>();
    for (CwSlideEntity slide : slides) {
      CwOutlinePageEntity page = outlineById.get(slide.getOutlinePageId());
      String pageRole = page != null ? page.getPageRole() : "content";
      String footer = buildFooter(project.getTitle(), page, sectionsByRef);
      renderSlides.add(new RenderSlide(slide.getLayoutId(), blocksBySlide.get(slide.getId()),
          slide.getSpeakerNotes(), footer, pageRole));
    }
    byte[] pptx = PptxRenderer.render(renderSlides);

    // 3) 回读验证(方案 §10.6)
    PptxRenderer.verify(pptx, renderSlides.size());

    // 4) 落 file 包(原生 pptx,非整页图片)
    FileVO stored = fileService.storeFromBytesForTenant(tenantId, pptx,
        project.getTitle() + ".pptx", PptxRenderer.PPTX_CONTENT_TYPE, "private");
    log.info("课件导出完成: projectId={}, fileId={}, pages={}, warnings={}", projectId,
        stored.id(), slides.size(), warnings.size());
    return new ExportResult(false, List.of(), stored.id(), warnings);
  }

  /** 页脚来源文本(方案 §10.9):来源:《标题》小节名(取前 2 个,超出省略号;映射不到用 locator) */
  public static String buildFooter(String deckTitle, CwOutlinePageEntity page,
                            Map<String, CwSourceSectionEntity> sectionsByRef) {
    if (page == null || page.getSourceRefs() == null) {
      return null;
    }
    List<String> refs = new ArrayList<>();
    try {
      for (JsonNode node : new ObjectMapper().readTree(page.getSourceRefs())) {
        if (node.isTextual()) {
          refs.add(node.asText());
        }
      }
    } catch (Exception e) {
      return null;
    }
    if (refs.isEmpty()) {
      return null;
    }
    List<String> names = new ArrayList<>();
    for (String ref : refs) {
      CwSourceSectionEntity section = sectionsByRef.get(ref);
      if (section == null) {
        continue;
      }
      String name = section.getHeading() != null && !section.getHeading().isBlank()
          ? section.getHeading() : section.getLocator();
      if (name != null && !name.isBlank()) {
        names.add(name);
      }
    }
    if (names.isEmpty()) {
      return null;
    }
    String joined = String.join("、", names.subList(0, Math.min(2, names.size())))
        + (names.size() > 2 ? " 等" : "");
    return "来源:《" + deckTitle + "》" + joined;
  }

  private List<SlideBlock> parseBlocks(String json) {
    List<SlideBlock> blocks = new ArrayList<>();
    if (json == null || json.isBlank()) {
      return blocks;
    }
    try {
      for (JsonNode node : objectMapper.readTree(json)) {
        blocks.add(new SlideBlock(
            textOr(node, "slotId"), textOr(node, "type"), textOr(node, "text"),
            stringList(node.path("items")), stringList(node.path("header")),
            rowsList(node.path("rows")), textOr(node, "value"), textOr(node, "label"),
            textOr(node, "note")));
      }
    } catch (Exception e) {
      log.warn("页 blocks 解析失败: {}", e.getMessage());
    }
    return blocks;
  }

  private static StructureIssue withPosition(StructureIssue issue, CwSlideEntity slide) {
    return new StructureIssue(issue.severity(), issue.code(), issue.slotId(),
        "第 " + slide.getPosition() + " 页 " + issue.message());
  }

  private static String textOr(JsonNode node, String field) {
    JsonNode v = node.path(field);
    return v.isTextual() ? v.asText() : null;
  }

  private static List<String> stringList(JsonNode node) {
    List<String> out = new ArrayList<>();
    if (node.isArray()) {
      node.forEach(n -> out.add(n.isTextual() ? n.asText() : n.toString()));
    }
    return out;
  }

  private static List<List<String>> rowsList(JsonNode node) {
    List<List<String>> out = new ArrayList<>();
    if (node.isArray()) {
      node.forEach(row -> out.add(stringList(row)));
    }
    return out;
  }
}
