package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.PreparationAssessorTaskDraftReq;
import com.guoyu.platform.certification.dto.PreparationAssessorTaskReviewReq;
import com.guoyu.platform.certification.dto.PreparationAssessorWorkbenchQueryReq;
import com.guoyu.platform.certification.service.CertificationPreparationApplicationService;
import com.guoyu.platform.certification.vo.PreparationAssessorTaskVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/preparation")
public class CertificationAssessorWorkbenchController {

  private final CertificationPreparationApplicationService service;

  public CertificationAssessorWorkbenchController(CertificationPreparationApplicationService service) {
    this.service = service;
  }

  @GetMapping("/assessor-workbench/tasks")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.workbench')")
  public ApiResponse<PageResponse<PreparationAssessorTaskVO>> pageTasks(
      @Valid @ModelAttribute PreparationAssessorWorkbenchQueryReq req
  ) {
    return ApiResponse.ok(service.pageWorkbenchTasks(req));
  }

  @GetMapping("/assessor-workbench/tasks/{taskId}")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.workbench')")
  public ApiResponse<PreparationAssessorTaskVO> getTask(@PathVariable Long taskId) {
    return ApiResponse.ok(service.getWorkbenchTask(taskId));
  }

  @PutMapping("/assessor-workbench/tasks/{taskId}/draft")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.score')")
  @AuditLog(module = "certification", action = "assessor-workbench:draft", targetType = "certification_assessor_task")
  public ApiResponse<PreparationAssessorTaskVO> saveDraft(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationAssessorTaskDraftReq req
  ) {
    return ApiResponse.ok(service.saveAssessorTaskDraft(taskId, req));
  }

  @PostMapping("/assessor-workbench/tasks/{taskId}/submit")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.score')")
  @AuditLog(module = "certification", action = "assessor-workbench:submit", targetType = "certification_assessor_task")
  public ApiResponse<PreparationAssessorTaskVO> submit(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationAssessorTaskDraftReq req
  ) {
    return ApiResponse.ok(service.submitAssessorTask(taskId, req));
  }

  @PostMapping("/assessor-workbench/tasks/{taskId}/review")
  @PreAuthorize("hasAuthority('certification.preparation.assessor.review')")
  @AuditLog(module = "certification", action = "assessor-workbench:review", targetType = "certification_assessor_task")
  public ApiResponse<PreparationAssessorTaskVO> review(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationAssessorTaskReviewReq req
  ) {
    return ApiResponse.ok(service.reviewAssessorTask(taskId, req));
  }
}
