package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.PreparationTaskQueryReq;
import com.guoyu.platform.certification.service.CertificationPreparationApplicationService;
import com.guoyu.platform.certification.vo.PreparationCompleteCheckVO;
import com.guoyu.platform.certification.vo.PreparationTaskOverviewVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/preparation")
public class CertificationPreparationController {

  private final CertificationPreparationApplicationService service;

  public CertificationPreparationController(CertificationPreparationApplicationService service) {
    this.service = service;
  }

  @GetMapping("/tasks")
  @PreAuthorize("hasAuthority('certification.preparation.task.view')")
  public ApiResponse<PageResponse<PreparationTaskOverviewVO>> pageTasks(@Valid @ModelAttribute PreparationTaskQueryReq req) {
    return ApiResponse.ok(service.pageTasks(req));
  }

  @GetMapping("/tasks/{taskId}/overview")
  @PreAuthorize("hasAuthority('certification.preparation.task.view')")
  public ApiResponse<PreparationTaskOverviewVO> overview(@PathVariable Long taskId) {
    return ApiResponse.ok(service.overview(taskId));
  }

  @GetMapping("/tasks/{taskId}/complete-check")
  @PreAuthorize("hasAuthority('certification.preparation.task.view')")
  public ApiResponse<PreparationCompleteCheckVO> completeCheck(@PathVariable Long taskId) {
    return ApiResponse.ok(service.completeCheck(taskId));
  }

  @PostMapping("/tasks/{taskId}/complete")
  @PreAuthorize("hasAuthority('certification.preparation.complete')")
  @AuditLog(module = "certification", action = "preparation:complete", targetType = "certification_task")
  public ApiResponse<PreparationCompleteCheckVO> complete(@PathVariable Long taskId) {
    return ApiResponse.ok(service.complete(taskId));
  }
}
