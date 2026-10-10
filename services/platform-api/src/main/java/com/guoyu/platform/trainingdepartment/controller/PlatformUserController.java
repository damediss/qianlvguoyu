package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserQueryReq;
import com.guoyu.platform.trainingdepartment.dto.PlatformUserSaveReq;
import com.guoyu.platform.trainingdepartment.service.PlatformUserApplicationService;
import com.guoyu.platform.trainingdepartment.vo.PlatformUserVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training")
public class PlatformUserController {

  private final PlatformUserApplicationService platformUserApplicationService;

  public PlatformUserController(PlatformUserApplicationService platformUserApplicationService) {
    this.platformUserApplicationService = platformUserApplicationService;
  }

  @GetMapping("/platform-users")
  @PreAuthorize("hasAuthority('train:user-info:view')")
  public ApiResponse<PageResponse<PlatformUserVO>> page(@Valid @ModelAttribute PlatformUserQueryReq req) {
    return ApiResponse.ok(platformUserApplicationService.page(req));
  }

  @PostMapping("/platform-users")
  @PreAuthorize("hasAnyAuthority('train:user-info:create', 'train:user-info:update')")
  @AuditLog(module = "training", action = "platform-user:save", targetType = "platform_user")
  public ApiResponse<PlatformUserVO> save(@Valid @RequestBody PlatformUserSaveReq req) {
    return ApiResponse.ok(platformUserApplicationService.save(req));
  }

  @DeleteMapping("/platform-users/{id}")
  @PreAuthorize("hasAuthority('train:user-info:delete')")
  @AuditLog(module = "training", action = "platform-user:delete", targetType = "platform_user")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    platformUserApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
