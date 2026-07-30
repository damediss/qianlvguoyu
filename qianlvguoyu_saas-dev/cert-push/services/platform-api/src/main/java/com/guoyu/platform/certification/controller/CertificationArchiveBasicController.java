package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.service.CertificationArchiveApplicationService;
import com.guoyu.platform.certification.vo.CertificationArchiveBasicSnapshotVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/certification/archive")
public class CertificationArchiveBasicController {
  private final CertificationArchiveApplicationService service;

  public CertificationArchiveBasicController(CertificationArchiveApplicationService service) {
    this.service = service;
  }

  @PostMapping("/tasks/{taskId}/basic")
  @PreAuthorize("hasAuthority('certification.archive.basic.create')")
  @AuditLog(module = "certification", action = "archive-basic:create", targetType = "certification_archive_basic_snapshot")
  public ApiResponse<CertificationArchiveBasicSnapshotVO> archiveBasic(@PathVariable Long taskId) {
    return ApiResponse.ok(service.archiveBasic(taskId));
  }

  @GetMapping("/records/{archiveId}/basic")
  @PreAuthorize("hasAuthority('certification.archive.basic.view')")
  public ApiResponse<CertificationArchiveBasicSnapshotVO> getBasic(@PathVariable Long archiveId) {
    return ApiResponse.ok(service.getBasicSnapshot(archiveId));
  }
}
