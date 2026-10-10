package com.guoyu.platform.courseware.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.courseware.entity.CwOutlinePageEntity;
import com.guoyu.platform.courseware.service.CoursewareGenerationService;
import com.guoyu.platform.courseware.service.CoursewareOutlineService;
import com.guoyu.platform.courseware.vo.CoursewareVOs.OutlinePageVO;
import com.guoyu.platform.security.SecurityContext;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 大纲(查看/编辑/增删/调序/确认) */
@Slf4j
@RestController
@RequestMapping("/courseware")
public class CoursewareOutlineController {

  private final CoursewareOutlineService outlineService;
  private final CoursewareGenerationService generationService;

  public CoursewareOutlineController(CoursewareOutlineService outlineService,
                                     CoursewareGenerationService generationService) {
    this.outlineService = outlineService;
    this.generationService = generationService;
  }

  @GetMapping("/projects/{id}/outline")
  @PreAuthorize("hasAuthority('courseware:project:list')")
  public ApiResponse<List<OutlinePageVO>> getOutline(@PathVariable Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<CwOutlinePageEntity> pages = outlineService.listPages(tenantId, id);
    return ApiResponse.ok(pages.stream().map(this::toVO).toList());
  }

  public record UpdateOutlinePageReq(String title, String objective, List<String> keyPoints,
                                     String chapterTitle, String layoutId) {
  }

  @PutMapping("/outline/pages/{pageId}")
  @PreAuthorize("hasAuthority('courseware:project:confirm')")
  @AuditLog(module = "courseware", action = "outline:update", targetType = "cw_outline_page")
  public ApiResponse<OutlinePageVO> updatePage(@PathVariable Long pageId,
                                               @RequestBody UpdateOutlinePageReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return ApiResponse.ok(toVO(outlineService.updatePage(tenantId, pageId, req.title(),
        req.objective(), req.keyPoints(), req.chapterTitle(), req.layoutId())));
  }

  public record AddOutlinePageReq(Integer chapterNo, Long afterPageId, String title,
                                  String objective, List<String> keyPoints) {
  }

  @PostMapping("/projects/{id}/outline/pages")
  @PreAuthorize("hasAuthority('courseware:project:confirm')")
  @AuditLog(module = "courseware", action = "outline:add", targetType = "cw_outline_page")
  public ApiResponse<OutlinePageVO> addPage(@PathVariable Long id,
                                            @RequestBody AddOutlinePageReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    return ApiResponse.ok(toVO(outlineService.addPage(tenantId, id, userId, req.chapterNo(),
        req.afterPageId(), req.title(), req.objective(), req.keyPoints())));
  }

  @DeleteMapping("/outline/pages/{pageId}")
  @PreAuthorize("hasAuthority('courseware:project:confirm')")
  @AuditLog(module = "courseware", action = "outline:delete", targetType = "cw_outline_page")
  public ApiResponse<Void> deletePage(@PathVariable Long pageId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    outlineService.deletePage(tenantId, pageId);
    return ApiResponse.ok();
  }

  public record ReorderReq(List<Long> orderedIds) {
  }

  @PostMapping("/projects/{id}/outline/reorder")
  @PreAuthorize("hasAuthority('courseware:project:confirm')")
  public ApiResponse<Void> reorder(@PathVariable Long id, @RequestBody ReorderReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    outlineService.reorder(tenantId, id, req.orderedIds());
    return ApiResponse.ok();
  }

  /** 确认大纲 → 快照固化 → 事务提交后派发页级生成(先提交后投递,防 Worker 抢跑读不到未提交行) */
  @PostMapping("/projects/{id}/confirm")
  @PreAuthorize("hasAuthority('courseware:project:confirm')")
  @AuditLog(module = "courseware", action = "outline:confirm", targetType = "cw_project")
  public ApiResponse<Void> confirm(@PathVariable Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    outlineService.confirm(tenantId, userId, id);
    generationService.dispatchGeneration(tenantId, userId, id);
    return ApiResponse.ok();
  }

  private OutlinePageVO toVO(CwOutlinePageEntity entity) {
    return new OutlinePageVO(entity.getId(), entity.getChapterNo(), entity.getChapterTitle(),
        entity.getPosition(), entity.getTitle(), entity.getObjective(),
        readStringList(entity.getKeyPoints()), entity.getPageRole(), entity.getLayoutId(),
        readStringList(entity.getSourceRefs()),
        entity.getChapterNo() != null && entity.getChapterNo() == 0);
  }

  private static List<String> readStringList(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      List<String> out = new java.util.ArrayList<>();
      for (com.fasterxml.jackson.databind.JsonNode node : new com.fasterxml.jackson.databind.ObjectMapper().readTree(json)) {
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
