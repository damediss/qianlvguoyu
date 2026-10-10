package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.PreparationChecklistUpdateReq;
import com.guoyu.platform.certification.service.CertificationPreparationApplicationService;
import com.guoyu.platform.certification.vo.PreparationChecklistVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/preparation")
public class CertificationPreparationChecklistController {

  private final CertificationPreparationApplicationService service;

  public CertificationPreparationChecklistController(CertificationPreparationApplicationService service) {
    this.service = service;
  }

  @GetMapping("/tasks/{taskId}/checklist")
  @PreAuthorize("hasAuthority('certification.preparation.checklist.view')")
  public ApiResponse<PreparationChecklistVO> getChecklist(@PathVariable Long taskId) {
    return ApiResponse.ok(service.getChecklist(taskId));
  }

  @PutMapping("/tasks/{taskId}/checklist")
  @PreAuthorize("hasAuthority('certification.preparation.checklist.confirm')")
  @AuditLog(module = "certification", action = "preparation-checklist:confirm", targetType = "certification_task_checklist_item")
  public ApiResponse<PreparationChecklistVO> updateChecklist(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationChecklistUpdateReq req
  ) {
    return ApiResponse.ok(service.updateChecklist(taskId, req));
  }
}
