package com.guoyu.platform.system.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.system.dto.RoleCreateReq;
import com.guoyu.platform.system.dto.RolePermissionBindReq;
import com.guoyu.platform.system.dto.RoleQueryReq;
import com.guoyu.platform.system.dto.RoleUpdateReq;
import com.guoyu.platform.system.service.RoleApplicationService;
import com.guoyu.platform.system.vo.RoleVO;
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
@RequestMapping("/system/roles")
public class RoleController {

  private final RoleApplicationService roleApplicationService;

  public RoleController(RoleApplicationService roleApplicationService) {
    this.roleApplicationService = roleApplicationService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('role:view')")
  public ApiResponse<PageResponse<RoleVO>> page(@Valid @ModelAttribute RoleQueryReq req) {
    return ApiResponse.ok(roleApplicationService.page(req));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('role:create')")
  @AuditLog(module = "system", action = "role:create", targetType = "role")
  public ApiResponse<RoleVO> create(@Valid @RequestBody RoleCreateReq req) {
    return ApiResponse.ok(roleApplicationService.create(req));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('role:view')")
  public ApiResponse<RoleVO> detail(@PathVariable Long id) {
    return ApiResponse.ok(roleApplicationService.detail(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('role:update')")
  @AuditLog(module = "system", action = "role:update", targetType = "role")
  public ApiResponse<RoleVO> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateReq req) {
    return ApiResponse.ok(roleApplicationService.update(id, req));
  }

  @PutMapping("/{id}/permissions")
  @PreAuthorize("hasAuthority('role:update')")
  @AuditLog(module = "system", action = "role:bind-permissions", targetType = "role")
  public ApiResponse<RoleVO> bindPermissions(@PathVariable Long id, @Valid @RequestBody RolePermissionBindReq req) {
    return ApiResponse.ok(roleApplicationService.bindPermissions(id, req));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('role:delete')")
  @AuditLog(module = "system", action = "role:delete", targetType = "role")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    roleApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
