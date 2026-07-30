package com.guoyu.platform.system.controller;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.system.dto.TenantCreateReq;
import com.guoyu.platform.system.dto.TenantQueryReq;
import com.guoyu.platform.system.dto.TenantUpdateReq;
import com.guoyu.platform.system.service.TenantApplicationService;
import com.guoyu.platform.system.vo.TenantVO;
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
@RequestMapping("/system/tenants")
@Slf4j
public class TenantController {

  private final TenantApplicationService tenantApplicationService;

  public TenantController(TenantApplicationService tenantApplicationService) {
    this.tenantApplicationService = tenantApplicationService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('tenant:view')")
  public ApiResponse<PageResponse<TenantVO>> page(@Valid @ModelAttribute TenantQueryReq req) {
    return ApiResponse.ok(tenantApplicationService.page(req));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('tenant:create')")
  @AuditLog(module = "system", action = "tenant:create", targetType = "tenant")
  public ApiResponse<TenantVO> create(@Valid @RequestBody TenantCreateReq req) {
    return ApiResponse.ok(tenantApplicationService.create(req));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('tenant:view')")
  public ApiResponse<TenantVO> detail(@PathVariable Long id) {
    return ApiResponse.ok(tenantApplicationService.detail(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('tenant:update')")
  @AuditLog(module = "system", action = "tenant:update", targetType = "tenant")
  public ApiResponse<TenantVO> update(@PathVariable Long id, @Valid @RequestBody TenantUpdateReq req) {
    return ApiResponse.ok(tenantApplicationService.update(id, req));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('tenant:delete')")
  @AuditLog(module = "system", action = "tenant:delete", targetType = "tenant")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    tenantApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
