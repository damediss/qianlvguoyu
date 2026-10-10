package com.guoyu.platform.system.controller;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.system.dto.ResetPasswordReq;
import com.guoyu.platform.system.dto.UserCreateReq;
import com.guoyu.platform.system.dto.UserQueryReq;
import com.guoyu.platform.system.dto.UserUpdateReq;
import com.guoyu.platform.system.service.UserApplicationService;
import com.guoyu.platform.system.vo.UserVO;
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
@RequestMapping("/system/users")
@Slf4j
public class UserController {

  private final UserApplicationService userApplicationService;

  public UserController(UserApplicationService userApplicationService) {
    this.userApplicationService = userApplicationService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('user:view')")
  public ApiResponse<PageResponse<UserVO>> page(@Valid @ModelAttribute UserQueryReq req) {
    return ApiResponse.ok(userApplicationService.page(req));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('user:create')")
  @AuditLog(module = "system", action = "user:create", targetType = "user")
  public ApiResponse<UserVO> create(@Valid @RequestBody UserCreateReq req) {
    return ApiResponse.ok(userApplicationService.create(req));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('user:view')")
  public ApiResponse<UserVO> detail(@PathVariable Long id) {
    return ApiResponse.ok(userApplicationService.detail(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('user:update')")
  @AuditLog(module = "system", action = "user:update", targetType = "user")
  public ApiResponse<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateReq req) {
    return ApiResponse.ok(userApplicationService.update(id, req));
  }

  @PostMapping("/{id}/reset-password")
  @PreAuthorize("hasAuthority('user:update')")
  @AuditLog(module = "system", action = "user:reset-password", targetType = "user")
  public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordReq req) {
    userApplicationService.resetPassword(id, req);
    return ApiResponse.ok();
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('user:delete')")
  @AuditLog(module = "system", action = "user:delete", targetType = "user")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    userApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
