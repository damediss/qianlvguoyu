package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserCreateReq;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserQueryReq;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserUpdateReq;
import com.guoyu.platform.trainingdepartment.service.PlatformUserService;
import com.guoyu.platform.trainingdepartment.vo.PlatformUserVO;
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

/**
 * 平台用户管理
 */
@Validated
@RestController
@RequestMapping("/training")
public class PlatformUserController {

  private final PlatformUserService service;

  public PlatformUserController(PlatformUserService service) {
    this.service = service;
  }

  /** 分页查询平台用户列表 */
  @GetMapping("/platform-users")
  @PreAuthorize("hasAuthority('train:platform-user:view')")
  public ApiResponse<PageResponse<PlatformUserVO>> page(@Valid @ModelAttribute PlatformUserQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  /** 根据ID查询平台用户详情 */
  @GetMapping("/platform-users/{id}")
  @PreAuthorize("hasAuthority('train:platform-user:view')")
  public ApiResponse<PlatformUserVO> getById(@PathVariable Long id) {
    return ApiResponse.ok(service.getById(id));
  }

  /** 新增平台用户 */
  @PostMapping("/platform-users")
  @PreAuthorize("hasAuthority('train:platform-user:create')")
  @AuditLog(module = "training", action = "platform-user:create", targetType = "platform_user")
  public ApiResponse<PlatformUserVO> create(@Valid @RequestBody PlatformUserCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  /** 修改平台用户 */
  @PutMapping("/platform-users/{id}")
  @PreAuthorize("hasAuthority('train:platform-user:update')")
  @AuditLog(module = "training", action = "platform-user:update", targetType = "platform_user")
  public ApiResponse<PlatformUserVO> update(@PathVariable Long id, @Valid @RequestBody PlatformUserUpdateReq req) {
    return ApiResponse.ok(service.update(id, req));
  }

  /** 删除平台用户 */
  @DeleteMapping("/platform-users/{id}")
  @PreAuthorize("hasAuthority('train:platform-user:delete')")
  @AuditLog(module = "training", action = "platform-user:delete", targetType = "platform_user")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    service.delete(id);

    return ApiResponse.ok();
  }
}
