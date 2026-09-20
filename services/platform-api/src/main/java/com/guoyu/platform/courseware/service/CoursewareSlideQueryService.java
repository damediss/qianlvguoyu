package com.guoyu.platform.courseware.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.courseware.domain.StructureIssue;
import com.guoyu.platform.courseware.entity.CwOutlinePageEntity;
import com.guoyu.platform.courseware.entity.CwProjectEntity;
import com.guoyu.platform.courseware.entity.CwSlideEntity;
import com.guoyu.platform.courseware.mapper.CwOutlinePageMapper;
import com.guoyu.platform.courseware.mapper.CwProjectMapper;
import com.guoyu.platform.courseware.mapper.CwSlideMapper;
import com.guoyu.platform.courseware.vo.CoursewareVOs.IssueVO;
import com.guoyu.platform.courseware.vo.CoursewareVOs.ProgressVO;
import com.guoyu.platform.courseware.vo.CoursewareVOs.SlideBriefVO;
import com.guoyu.platform.courseware.vo.CoursewareVOs.SlideDetailVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** 页查询与进度聚合(进度轮询接口的数据源,方案 §11 第③步) */
@Service
public class CoursewareSlideQueryService {

  private final CwProjectMapper projectMapper;
  private final CwSlideMapper slideMapper;
  private final CwOutlinePageMapper outlineMapper;
  private final ObjectMapper objectMapper;

  public CoursewareSlideQueryService(CwProjectMapper projectMapper, CwSlideMapper slideMapper,
                                     CwOutlinePageMapper outlineMapper, ObjectMapper objectMapper) {
    this.projectMapper = projectMapper;
    this.slideMapper = slideMapper;
    this.outlineMapper = outlineMapper;
    this.objectMapper = objectMapper;
  }

  public ProgressVO progress(Long tenantId, Long projectId) {
    CwProjectEntity project = requireProject(tenantId, projectId);
    List<SlideBriefVO> slides = listSlides(tenantId, projectId);
    return new ProgressVO(project.getStatus(), project.getTotalPages(), project.getReadyPages(),
        project.getFailedPages(), project.getError(), slides);
  }

  public List<SlideBriefVO> listSlides(Long tenantId, Long projectId) {
    requireProject(tenantId, projectId);
    List<CwSlideEntity> slides = slideMapper.selectList(new LambdaQueryWrapper<CwSlideEntity>()
        .eq(CwSlideEntity::getTenantId, tenantId)
        .eq(CwSlideEntity::getProjectId, projectId)
        .orderByAsc(CwSlideEntity::getPosition));
    Map<Long, CwOutlinePageEntity> outlineById = outlineMapper.selectList(
            new LambdaQueryWrapper<CwOutlinePageEntity>()
                .eq(CwOutlinePageEntity::getProjectId, projectId))
        .stream().collect(Collectors.toMap(CwOutlinePageEntity::getId, Function.identity()));
    return slides.stream().map(s -> {
      CwOutlinePageEntity page = outlineById.get(s.getOutlinePageId());
      return new SlideBriefVO(s.getId(), s.getPosition(),
          page != null ? page.getTitle() : null, page != null ? page.getPageRole() : null,
          s.getStatus(), s.getError());
    }).toList();
  }

  public SlideDetailVO slideDetail(Long tenantId, Long slideId) {
    CwSlideEntity slide = slideMapper.selectById(slideId);
    if (slide == null || !tenantId.equals(slide.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "页面不存在");
    }
    CwOutlinePageEntity page = slide.getOutlinePageId() != null
        ? outlineMapper.selectById(slide.getOutlinePageId()) : null;
    return new SlideDetailVO(slide.getId(), slide.getPosition(),
        page != null ? page.getTitle() : null, page != null ? page.getPageRole() : null,
        slide.getLayoutId(), readTree(slide.getBlocks()), slide.getSpeakerNotes(),
        readTree(slide.getIssues()), slide.getStatus(), slide.getError(), slide.getRevision());
  }

  /** 项目质量报告(导出前检查透出;方案 §10.5) */
  public List<IssueVO> projectIssues(Long tenantId, Long projectId) {
    requireProject(tenantId, projectId);
    List<CwSlideEntity> slides = slideMapper.selectList(new LambdaQueryWrapper<CwSlideEntity>()
        .eq(CwSlideEntity::getTenantId, tenantId)
        .eq(CwSlideEntity::getProjectId, projectId)
        .orderByAsc(CwSlideEntity::getPosition));
    List<IssueVO> out = new ArrayList<>();
    for (CwSlideEntity slide : slides) {
      for (StructureIssue issue : readIssues(slide.getIssues())) {
        out.add(new IssueVO(issue.severity(), issue.code(), issue.slotId(),
            "第 " + slide.getPosition() + " 页 " + issue.message()));
      }
    }
    return out;
  }

  private CwProjectEntity requireProject(Long tenantId, Long projectId) {
    CwProjectEntity project = projectMapper.selectById(projectId);
    if (project == null || !tenantId.equals(project.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "课件项目不存在");
    }
    return project;
  }

  private JsonNode readTree(String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readTree(json);
    } catch (Exception e) {
      return null;
    }
  }

  private List<StructureIssue> readIssues(String json) {
    List<StructureIssue> out = new ArrayList<>();
    if (json == null || json.isBlank()) {
      return out;
    }
    try {
      for (JsonNode node : objectMapper.readTree(json)) {
        out.add(new StructureIssue(node.path("severity").asText(null),
            node.path("code").asText(null), node.path("slotId").asText(null),
            node.path("message").asText("")));
      }
    } catch (Exception e) {
      // issues JSON 损坏时跳过,不影响主链路
    }
    return out;
  }
}
