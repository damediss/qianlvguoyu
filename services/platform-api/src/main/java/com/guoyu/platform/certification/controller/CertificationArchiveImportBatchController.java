package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.CertificationArchiveImportBatchQueryReq;
import com.guoyu.platform.certification.dto.CertificationArchiveTaskQueryReq;
import com.guoyu.platform.certification.service.CertificationArchiveApplicationService;
import com.guoyu.platform.certification.vo.CertificationArchiveImportBatchVO;
import com.guoyu.platform.certification.vo.CertificationArchiveImportErrorVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/certification/archive")
public class CertificationArchiveImportBatchController {
  private final CertificationArchiveApplicationService service;

  public CertificationArchiveImportBatchController(CertificationArchiveApplicationService service) {
    this.service = service;
  }

  @PostMapping("/records/{archiveId}/candidate-materials/import")
  @PreAuthorize("hasAuthority('certification.archive.candidate.import')")
  @AuditLog(module = "certification", action = "archive-candidate-material:import", targetType = "certification_archive_import_batch")
  public ApiResponse<CertificationArchiveImportBatchVO> importCandidateMaterials(
      @PathVariable Long archiveId,
      @RequestParam("file") MultipartFile file
  ) {
    return ApiResponse.ok(service.importCandidateMaterials(archiveId, file));
  }

  @GetMapping("/import-batches")
  @PreAuthorize("hasAuthority('certification.archive.import.view')")
  public ApiResponse<PageResponse<CertificationArchiveImportBatchVO>> pageImportBatches(
      @Valid @ModelAttribute CertificationArchiveImportBatchQueryReq req
  ) {
    return ApiResponse.ok(service.pageImportBatches(req));
  }

  @GetMapping("/import-batches/{batchId}")
  @PreAuthorize("hasAuthority('certification.archive.import.view')")
  public ApiResponse<CertificationArchiveImportBatchVO> getImportBatch(@PathVariable Long batchId) {
    return ApiResponse.ok(service.getImportBatch(batchId));
  }

  @GetMapping("/import-batches/{batchId}/errors")
  @PreAuthorize("hasAuthority('certification.archive.import.view')")
  public ApiResponse<PageResponse<CertificationArchiveImportErrorVO>> pageImportErrors(
      @PathVariable Long batchId,
      @Valid @ModelAttribute CertificationArchiveTaskQueryReq req
  ) {
    return ApiResponse.ok(service.pageImportErrors(batchId, req));
  }

  @PostMapping("/import-batches/{batchId}/confirm")
  @PreAuthorize("hasAuthority('certification.archive.import.confirm')")
  @AuditLog(module = "certification", action = "archive-import:confirm", targetType = "certification_archive_import_batch")
  public ApiResponse<CertificationArchiveImportBatchVO> confirmImport(@PathVariable Long batchId) {
    return ApiResponse.ok(service.confirmImport(batchId));
  }

  @PostMapping("/import-batches/{batchId}/cancel")
  @PreAuthorize("hasAuthority('certification.archive.import.cancel')")
  @AuditLog(module = "certification", action = "archive-import:cancel", targetType = "certification_archive_import_batch")
  public ApiResponse<CertificationArchiveImportBatchVO> cancelImport(@PathVariable Long batchId) {
    return ApiResponse.ok(service.cancelImport(batchId));
  }
}
