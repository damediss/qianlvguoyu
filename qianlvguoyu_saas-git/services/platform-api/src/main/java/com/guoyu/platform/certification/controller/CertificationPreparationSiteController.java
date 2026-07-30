package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.PreparationExamSiteQueryReq;
import com.guoyu.platform.certification.dto.PreparationExamSiteSaveReq;
import com.guoyu.platform.certification.dto.PreparationTaskExamSiteSaveReq;
import com.guoyu.platform.certification.service.CertificationPreparationApplicationService;
import com.guoyu.platform.certification.vo.PreparationExamSiteVO;
import com.guoyu.platform.certification.vo.PreparationTaskExamSiteVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/preparation")
public class CertificationPreparationSiteController {

  private final CertificationPreparationApplicationService service;

  public CertificationPreparationSiteController(CertificationPreparationApplicationService service) {
    this.service = service;
  }

  @GetMapping("/exam-sites")
  @PreAuthorize("hasAuthority('certification.preparation.site.view')")
  public ApiResponse<PageResponse<PreparationExamSiteVO>> pageExamSites(@Valid @ModelAttribute PreparationExamSiteQueryReq req) {
    return ApiResponse.ok(service.pageExamSites(req));
  }

  @PostMapping("/exam-sites")
  @PreAuthorize("hasAuthority('certification.preparation.site.edit')")
  @AuditLog(module = "certification", action = "preparation-site:create", targetType = "certification_exam_site")
  public ApiResponse<PreparationExamSiteVO> createExamSite(@Valid @RequestBody PreparationExamSiteSaveReq req) {
    return ApiResponse.ok(service.createExamSite(req));
  }

  @PutMapping("/exam-sites/{siteId}")
  @PreAuthorize("hasAuthority('certification.preparation.site.edit')")
  @AuditLog(module = "certification", action = "preparation-site:update", targetType = "certification_exam_site")
  public ApiResponse<PreparationExamSiteVO> updateExamSite(
      @PathVariable Long siteId,
      @Valid @RequestBody PreparationExamSiteSaveReq req
  ) {
    return ApiResponse.ok(service.updateExamSite(siteId, req));
  }

  @GetMapping("/tasks/{taskId}/exam-sites")
  @PreAuthorize("hasAuthority('certification.preparation.site.view')")
  public ApiResponse<List<PreparationTaskExamSiteVO>> listTaskExamSites(@PathVariable Long taskId) {
    return ApiResponse.ok(service.listTaskExamSites(taskId));
  }

  @PostMapping("/tasks/{taskId}/exam-sites")
  @PreAuthorize("hasAuthority('certification.preparation.site.assign')")
  @AuditLog(module = "certification", action = "preparation-site:assign", targetType = "certification_task_exam_site")
  public ApiResponse<PreparationTaskExamSiteVO> assignExamSite(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationTaskExamSiteSaveReq req
  ) {
    return ApiResponse.ok(service.assignExamSite(taskId, req));
  }

  @DeleteMapping("/tasks/{taskId}/exam-sites/{taskSiteId}")
  @PreAuthorize("hasAuthority('certification.preparation.site.assign')")
  @AuditLog(module = "certification", action = "preparation-site:remove", targetType = "certification_task_exam_site")
  public ApiResponse<Void> removeTaskExamSite(@PathVariable Long taskId, @PathVariable Long taskSiteId) {
    service.removeTaskExamSite(taskId, taskSiteId);
    return ApiResponse.ok();
  }
}
