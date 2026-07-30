package com.guoyu.platform.system.controller;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.system.service.PermissionApplicationService;
import com.guoyu.platform.system.vo.PermissionItemVO;
import com.guoyu.platform.system.vo.PermissionMatrixVO;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/system/permissions")
@Slf4j
public class PermissionController {

  private final PermissionApplicationService permissionApplicationService;

  public PermissionController(PermissionApplicationService permissionApplicationService) {
    this.permissionApplicationService = permissionApplicationService;
  }

  @GetMapping("/matrix")
  @PreAuthorize("hasAuthority('role:view')")
  public ApiResponse<List<PermissionMatrixVO>> matrix() {
    return ApiResponse.ok(permissionApplicationService.matrix());
  }

  @GetMapping
  @PreAuthorize("hasAuthority('role:view')")
  public ApiResponse<List<PermissionItemVO>> list() {
    return ApiResponse.ok(permissionApplicationService.listFlat());
  }
}
