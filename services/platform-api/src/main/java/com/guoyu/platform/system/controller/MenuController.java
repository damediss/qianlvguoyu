package com.guoyu.platform.system.controller;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.system.dto.MenuCreateReq;
import com.guoyu.platform.system.dto.MenuUpdateReq;
import com.guoyu.platform.system.service.MenuApplicationService;
import com.guoyu.platform.system.vo.MenuTreeVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/system/menus")
@Slf4j
public class MenuController {

  private final MenuApplicationService menuApplicationService;

  public MenuController(MenuApplicationService menuApplicationService) {
    this.menuApplicationService = menuApplicationService;
  }

  @GetMapping("/tree")
  @PreAuthorize("hasAuthority('menu:view')")
  public ApiResponse<List<MenuTreeVO>> tree() {
    return ApiResponse.ok(menuApplicationService.tree());
  }

  @PostMapping
  @PreAuthorize("hasAuthority('menu:create')")
  @AuditLog(module = "system", action = "menu:create", targetType = "menu")
  public ApiResponse<MenuTreeVO> create(@Valid @RequestBody MenuCreateReq req) {
    return ApiResponse.ok(menuApplicationService.create(req));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('menu:update')")
  @AuditLog(module = "system", action = "menu:update", targetType = "menu")
  public ApiResponse<MenuTreeVO> update(@PathVariable Long id, @Valid @RequestBody MenuUpdateReq req) {
    return ApiResponse.ok(menuApplicationService.update(id, req));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('menu:delete')")
  @AuditLog(module = "system", action = "menu:delete", targetType = "menu")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    menuApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
