package com.guoyu.platform.certification.collection.export.controller;

import com.guoyu.platform.certification.collection.export.dto.CollectionExportPdfDTO;
import com.guoyu.platform.certification.collection.export.service.CollectionPdfExportService;
import com.guoyu.platform.certification.collection.export.vo.CollectionPdfExportVO;
import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/certification/collection/export")
public class CollectionExportController {

  private final CollectionPdfExportService service;

  public CollectionExportController(CollectionPdfExportService service) {
    this.service = service;
  }

  @PostMapping("/pdf")
  @PreAuthorize("hasAuthority('certification.practical.collection.export')")
  @AuditLog(module = "certification", action = "practical-collection:pdf-export", targetType = "file")
  public ApiResponse<CollectionPdfExportVO> exportPdf(@Valid @RequestBody CollectionExportPdfDTO request) {
    return ApiResponse.ok(service.export(request));
  }
}
