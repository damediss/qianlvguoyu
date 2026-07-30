package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.PreparationCandidateImportReq;
import com.guoyu.platform.certification.dto.PreparationCandidateQueryReq;
import com.guoyu.platform.certification.dto.PreparationCandidateSaveReq;
import com.guoyu.platform.certification.service.CertificationPreparationApplicationService;
import com.guoyu.platform.certification.vo.PreparationCandidateVO;
import com.guoyu.platform.certification.vo.PreparationImportBatchVO;
import com.guoyu.platform.certification.vo.PreparationImportErrorVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
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
public class CertificationPreparationCandidateController {

  private final CertificationPreparationApplicationService service;

  public CertificationPreparationCandidateController(CertificationPreparationApplicationService service) {
    this.service = service;
  }

  @PostMapping("/tasks/{taskId}/candidate-imports")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.import')")
  @AuditLog(module = "certification", action = "preparation-candidate:import", targetType = "certification_candidate")
  public ApiResponse<PreparationImportBatchVO> createImportBatch(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationCandidateImportReq req
  ) {
    return ApiResponse.ok(service.createImportBatch(taskId, req));
  }

  @GetMapping("/tasks/{taskId}/candidate-imports/{batchId}/errors")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.view')")
  public ApiResponse<PageResponse<PreparationImportErrorVO>> listImportErrors(
      @PathVariable Long taskId,
      @PathVariable Long batchId,
      @Valid @ModelAttribute PageQuery req
  ) {
    return ApiResponse.ok(service.pageImportErrors(taskId, batchId, req));
  }

  @GetMapping("/tasks/{taskId}/candidates")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.view')")
  public ApiResponse<PageResponse<PreparationCandidateVO>> pageCandidates(
      @PathVariable Long taskId,
      @Valid @ModelAttribute PreparationCandidateQueryReq req
  ) {
    return ApiResponse.ok(service.pageCandidates(taskId, req));
  }

  @PostMapping("/tasks/{taskId}/candidates")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.edit')")
  @AuditLog(module = "certification", action = "preparation-candidate:create", targetType = "certification_candidate")
  public ApiResponse<PreparationCandidateVO> createCandidate(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationCandidateSaveReq req
  ) {
    return ApiResponse.ok(service.createCandidate(taskId, req));
  }

  @PutMapping("/tasks/{taskId}/candidates/{candidateId}")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.edit')")
  @AuditLog(module = "certification", action = "preparation-candidate:update", targetType = "certification_candidate")
  public ApiResponse<PreparationCandidateVO> updateCandidate(
      @PathVariable Long taskId,
      @PathVariable Long candidateId,
      @Valid @RequestBody PreparationCandidateSaveReq req
  ) {
    return ApiResponse.ok(service.updateCandidate(taskId, candidateId, req));
  }

  @DeleteMapping("/tasks/{taskId}/candidates/{candidateId}")
  @PreAuthorize("hasAuthority('certification.preparation.candidate.delete')")
  @AuditLog(module = "certification", action = "preparation-candidate:delete", targetType = "certification_candidate")
  public ApiResponse<Void> deleteCandidate(@PathVariable Long taskId, @PathVariable Long candidateId) {
    service.deleteCandidate(taskId, candidateId);
    return ApiResponse.ok();
  }
}
