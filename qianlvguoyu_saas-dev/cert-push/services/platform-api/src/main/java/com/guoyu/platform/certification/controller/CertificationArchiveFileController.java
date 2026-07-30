package com.guoyu.platform.certification.controller;

import com.guoyu.platform.certification.service.CertificationArchiveApplicationService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/certification/archive")
public class CertificationArchiveFileController {
  private final CertificationArchiveApplicationService service;

  public CertificationArchiveFileController(CertificationArchiveApplicationService service) {
    this.service = service;
  }

  @GetMapping("/files/{fileId}/download")
  @PreAuthorize("hasAuthority('certification.archive.file.download')")
  public ResponseEntity<Resource> download(@PathVariable Long fileId) {
    return service.downloadFile(fileId);
  }

  @GetMapping("/files/{fileId}/preview")
  @PreAuthorize("hasAuthority('certification.archive.file.preview')")
  public ResponseEntity<Resource> preview(@PathVariable Long fileId) {
    return service.downloadFile(fileId);
  }
}
