package com.guoyu.platform.courseware.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.courseware.service.CoursewareExportService;
import com.guoyu.platform.courseware.service.CoursewareExportService.ExportResult;
import com.guoyu.platform.courseware.service.CoursewareGenerationService;
import com.guoyu.platform.courseware.service.CoursewareSlideQueryService;
import com.guoyu.platform.courseware.vo.CoursewareVOs.ExportResultVO;
import com.guoyu.platform.courseware.vo.CoursewareVOs.IssueVO;
import com.guoyu.platform.courseware.vo.CoursewareVOs.SlideBriefVO;
import com.guoyu.platform.courseware.vo.CoursewareVOs.SlideDetailVO;
import com.guoyu.platform.security.SecurityContext;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 生成页(列表/明细/重试/质量报告)与导出 */
@Slf4j
@RestController
@RequestMapping("/courseware")
public class CoursewareSlideController {

  private final CoursewareSlideQueryService slideQueryService;
  private final CoursewareGenerationService generationService;
  private final CoursewareExportService exportService;

  public CoursewareSlideController(CoursewareSlideQueryService slideQueryService,
                                   CoursewareGenerationService generationService,
                                   CoursewareExportService exportService) {
    this.slideQueryService = slideQueryService;
    this.generationService = generationService;
    this.exportService = exportService;
  }

  @GetMapping("/projects/{id}/slides")
  @PreAuthorize("hasAuthority('courseware:project:list')")
  public ApiResponse<List<SlideBriefVO>> slides(@PathVariable Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return ApiResponse.ok(slideQueryService.listSlides(tenantId, id));
  }

  @GetMapping("/slides/{slideId}")
  @PreAuthorize("hasAuthority('courseware:project:list')")
  public ApiResponse<SlideDetailVO> slideDetail(@PathVariable Long slideId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return ApiResponse.ok(slideQueryService.slideDetail(tenantId, slideId));
  }

  /** 失败页单独重试(仅 failed;方案 §11) */
  @PostMapping("/slides/{slideId}/retry")
  @PreAuthorize("hasAuthority('courseware:project:retry')")
  @AuditLog(module = "courseware", action = "slide:retry", targetType = "cw_slide")
  public ApiResponse<Void> retry(@PathVariable Long slideId) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    generationService.retrySlide(tenantId, userId, slideId);
    return ApiResponse.ok();
  }

  /** 项目质量报告(导出前检查透出) */
  @GetMapping("/projects/{id}/issues")
  @PreAuthorize("hasAuthority('courseware:project:list')")
  public ApiResponse<List<IssueVO>> issues(@PathVariable Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    return ApiResponse.ok(slideQueryService.projectIssues(tenantId, id));
  }

  /** 导出 PPTX:error 阻断返回问题清单;通过则落 file 包返回 fileId(方案 §10) */
  @PostMapping("/projects/{id}/export")
  @PreAuthorize("hasAuthority('courseware:project:export')")
  @AuditLog(module = "courseware", action = "project:export", targetType = "cw_project")
  public ApiResponse<ExportResultVO> export(@PathVariable Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    Long userId = SecurityContext.currentUser().getUserId();
    ExportResult result = exportService.export(tenantId, userId, id);
    return ApiResponse.ok(new ExportResultVO(result.blocked(),
        result.issues().stream().map(IssueVO::from).toList(), result.fileId(),
        result.warnings().stream().map(IssueVO::from).toList()));
  }
}
