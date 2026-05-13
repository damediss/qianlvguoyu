package com.guoyu.platform.system.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.system.dto.AuditLogQueryReq;
import com.guoyu.platform.system.service.AuditLogApplicationService;
import com.guoyu.platform.system.vo.AuditLogVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/system/audit-logs")
public class AuditLogController {

  private final AuditLogApplicationService auditLogApplicationService;

  public AuditLogController(AuditLogApplicationService auditLogApplicationService) {
    this.auditLogApplicationService = auditLogApplicationService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('audit-log:view')")
  public ApiResponse<PageResponse<AuditLogVO>> page(@Valid @ModelAttribute AuditLogQueryReq req) {
    return ApiResponse.ok(auditLogApplicationService.page(req));
  }
}
