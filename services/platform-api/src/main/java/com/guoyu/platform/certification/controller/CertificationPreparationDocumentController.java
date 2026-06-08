package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.PreparationCandidateDocumentSaveReq;
import com.guoyu.platform.certification.dto.PreparationDocumentQueryReq;
import com.guoyu.platform.certification.dto.PreparationDocumentReviewReq;
import com.guoyu.platform.certification.service.CertificationPreparationApplicationService;
import com.guoyu.platform.certification.vo.PreparationCandidateDocumentVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
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
public class CertificationPreparationDocumentController {

  private final CertificationPreparationApplicationService service;

  public CertificationPreparationDocumentController(CertificationPreparationApplicationService service) {
    this.service = service;
  }

  @GetMapping("/tasks/{taskId}/candidate-documents")
  @PreAuthorize("hasAuthority('certification.preparation.document.view')")
  public ApiResponse<PageResponse<PreparationCandidateDocumentVO>> pageCandidateDocuments(
      @PathVariable Long taskId,
      @Valid @ModelAttribute PreparationDocumentQueryReq req
  ) {
    return ApiResponse.ok(service.pageCandidateDocuments(taskId, req));
  }

  @PostMapping("/tasks/{taskId}/candidate-documents")
  @PreAuthorize("hasAuthority('certification.preparation.document.upload')")
  @AuditLog(module = "certification", action = "preparation-document:upload", targetType = "certification_candidate_document")
  public ApiResponse<PreparationCandidateDocumentVO> uploadCandidateDocument(
      @PathVariable Long taskId,
      @Valid @RequestBody PreparationCandidateDocumentSaveReq req
  ) {
    return ApiResponse.ok(service.saveCandidateDocument(taskId, req));
  }

  @GetMapping("/tasks/{taskId}/candidate-documents/{documentId}/download")
  @PreAuthorize("hasAuthority('certification.preparation.document.view')")
  public ResponseEntity<Resource> downloadCandidateDocument(
      @PathVariable Long taskId,
      @PathVariable Long documentId
  ) {
    return service.downloadCandidateDocument(taskId, documentId);
  }

  @DeleteMapping("/tasks/{taskId}/candidate-documents/{documentId}")
  @PreAuthorize("hasAuthority('certification.preparation.document.upload')")
  @AuditLog(module = "certification", action = "preparation-document:delete", targetType = "certification_candidate_document")
  public ApiResponse<Void> deleteCandidateDocument(
      @PathVariable Long taskId,
      @PathVariable Long documentId
  ) {
    service.deleteCandidateDocument(taskId, documentId);
    return ApiResponse.ok();
  }

  @PutMapping("/tasks/{taskId}/candidate-documents/{documentId}/review")
  @PreAuthorize("hasAuthority('certification.preparation.document.review')")
  @AuditLog(module = "certification", action = "preparation-document:review", targetType = "certification_candidate_document")
  public ApiResponse<PreparationCandidateDocumentVO> reviewCandidateDocument(
      @PathVariable Long taskId,
      @PathVariable Long documentId,
      @Valid @RequestBody PreparationDocumentReviewReq req
  ) {
    return ApiResponse.ok(service.reviewCandidateDocument(taskId, documentId, req));
  }
}
