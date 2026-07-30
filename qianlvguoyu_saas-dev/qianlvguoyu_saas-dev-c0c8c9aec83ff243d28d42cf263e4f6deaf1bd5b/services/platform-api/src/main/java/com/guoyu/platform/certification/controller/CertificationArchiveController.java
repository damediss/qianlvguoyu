package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.dto.CertificationArchiveConfirmReq;
import com.guoyu.platform.certification.dto.CertificationArchiveRecordQueryReq;
import com.guoyu.platform.certification.dto.CertificationArchiveTaskQueryReq;
import com.guoyu.platform.certification.service.CertificationArchiveApplicationService;
import com.guoyu.platform.certification.vo.CertificationArchiveCompleteCheckVO;
import com.guoyu.platform.certification.vo.CertificationArchiveDetailVO;
import com.guoyu.platform.certification.vo.CertificationArchiveRecordVO;
import com.guoyu.platform.certification.vo.CertificationArchiveTaskVO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/archive")
public class CertificationArchiveController {
  private final CertificationArchiveApplicationService service;

  public CertificationArchiveController(CertificationArchiveApplicationService service) {
    this.service = service;
  }

  @GetMapping("/tasks/available")
  @PreAuthorize("hasAuthority('certification.archive.task.view')")
  public ApiResponse<PageResponse<CertificationArchiveTaskVO>> pageAvailableTasks(@Valid @ModelAttribute CertificationArchiveTaskQueryReq req) {
    return ApiResponse.ok(service.pageAvailableTasks(req));
  }

  @GetMapping("/records")
  @PreAuthorize("hasAuthority('certification.archive.record.view')")
  public ApiResponse<PageResponse<CertificationArchiveRecordVO>> pageRecords(@Valid @ModelAttribute CertificationArchiveRecordQueryReq req) {
    return ApiResponse.ok(service.pageRecords(req));
  }

  @GetMapping("/records/{archiveId}")
  @PreAuthorize("hasAuthority('certification.archive.record.view')")
  public ApiResponse<CertificationArchiveDetailVO> getDetail(@PathVariable Long archiveId) {
    return ApiResponse.ok(service.getDetail(archiveId));
  }

  @GetMapping("/records/{archiveId}/complete-check")
  @PreAuthorize("hasAuthority('certification.archive.record.view')")
  public ApiResponse<CertificationArchiveCompleteCheckVO> completeCheck(@PathVariable Long archiveId) {
    return ApiResponse.ok(service.completeCheck(archiveId, false));
  }

  @PostMapping("/records/{archiveId}/confirm")
  @PreAuthorize("hasAuthority('certification.archive.import.confirm')")
  @AuditLog(module = "certification", action = "archive:confirm", targetType = "certification_archive_record")
  public ApiResponse<CertificationArchiveCompleteCheckVO> confirmArchive(
      @PathVariable Long archiveId,
      @RequestBody(required = false) CertificationArchiveConfirmReq req
  ) {
    return ApiResponse.ok(service.confirmArchive(archiveId, req));
  }
}
