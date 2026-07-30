package com.guoyu.platform.industrial.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.industrial.dto.IndustrialPlatformItemReq;
import com.guoyu.platform.industrial.dto.IndustrialPlatformQueryReq;
import com.guoyu.platform.industrial.service.IndustrialPlatformApplicationService;
import com.guoyu.platform.industrial.vo.IndustrialPlatformItemVO;
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
@RequestMapping("/industrial")
public class IndustrialPlatformController {

  private final IndustrialPlatformApplicationService service;

  public IndustrialPlatformController(IndustrialPlatformApplicationService service) {
    this.service = service;
  }

  @GetMapping("/platform-master")
  @PreAuthorize("hasAuthority('industrial:platform-master:view')")
  public ApiResponse<PageResponse<IndustrialPlatformItemVO>> pageMaster(
      @Valid @ModelAttribute IndustrialPlatformQueryReq req) {
    return ApiResponse.ok(service.page("master", req));
  }

  @GetMapping("/platform-master/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-master:view')")
  public ApiResponse<IndustrialPlatformItemVO> getMaster(@PathVariable Long id) {
    return ApiResponse.ok(service.getById("master", id));
  }

  @PostMapping("/platform-master")
  @PreAuthorize("hasAuthority('industrial:platform-master:create')")
  @AuditLog(module = "industrial", action = "platform-master:create", targetType = "industrial_platform_item")
  public ApiResponse<IndustrialPlatformItemVO> createMaster(@Valid @RequestBody IndustrialPlatformItemReq req) {
    return ApiResponse.ok(service.create("master", req));
  }

  @PutMapping("/platform-master/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-master:update')")
  @AuditLog(module = "industrial", action = "platform-master:update", targetType = "industrial_platform_item")
  public ApiResponse<IndustrialPlatformItemVO> updateMaster(@PathVariable Long id,
      @Valid @RequestBody IndustrialPlatformItemReq req) {
    return ApiResponse.ok(service.update("master", id, req));
  }

  @DeleteMapping("/platform-master/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-master:delete')")
  @AuditLog(module = "industrial", action = "platform-master:delete", targetType = "industrial_platform_item")
  public ApiResponse<Void> deleteMaster(@PathVariable Long id) {
    service.delete("master", id);
    return ApiResponse.ok();
  }

  @GetMapping("/platform-study")
  @PreAuthorize("hasAuthority('industrial:platform-study:view')")
  public ApiResponse<PageResponse<IndustrialPlatformItemVO>> pageStudy(
      @Valid @ModelAttribute IndustrialPlatformQueryReq req) {
    return ApiResponse.ok(service.page("study", req));
  }

  @GetMapping("/platform-study/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-study:view')")
  public ApiResponse<IndustrialPlatformItemVO> getStudy(@PathVariable Long id) {
    return ApiResponse.ok(service.getById("study", id));
  }

  @PostMapping("/platform-study")
  @PreAuthorize("hasAuthority('industrial:platform-study:create')")
  @AuditLog(module = "industrial", action = "platform-study:create", targetType = "industrial_platform_item")
  public ApiResponse<IndustrialPlatformItemVO> createStudy(@Valid @RequestBody IndustrialPlatformItemReq req) {
    return ApiResponse.ok(service.create("study", req));
  }

  @PutMapping("/platform-study/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-study:update')")
  @AuditLog(module = "industrial", action = "platform-study:update", targetType = "industrial_platform_item")
  public ApiResponse<IndustrialPlatformItemVO> updateStudy(@PathVariable Long id,
      @Valid @RequestBody IndustrialPlatformItemReq req) {
    return ApiResponse.ok(service.update("study", id, req));
  }

  @DeleteMapping("/platform-study/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-study:delete')")
  @AuditLog(module = "industrial", action = "platform-study:delete", targetType = "industrial_platform_item")
  public ApiResponse<Void> deleteStudy(@PathVariable Long id) {
    service.delete("study", id);
    return ApiResponse.ok();
  }

  @GetMapping("/platform-match")
  @PreAuthorize("hasAuthority('industrial:platform-match:view')")
  public ApiResponse<PageResponse<IndustrialPlatformItemVO>> pageMatch(
      @Valid @ModelAttribute IndustrialPlatformQueryReq req) {
    return ApiResponse.ok(service.page("match", req));
  }

  @GetMapping("/platform-match/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-match:view')")
  public ApiResponse<IndustrialPlatformItemVO> getMatch(@PathVariable Long id) {
    return ApiResponse.ok(service.getById("match", id));
  }

  @PostMapping("/platform-match")
  @PreAuthorize("hasAuthority('industrial:platform-match:create')")
  @AuditLog(module = "industrial", action = "platform-match:create", targetType = "industrial_platform_item")
  public ApiResponse<IndustrialPlatformItemVO> createMatch(@Valid @RequestBody IndustrialPlatformItemReq req) {
    return ApiResponse.ok(service.create("match", req));
  }

  @PutMapping("/platform-match/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-match:update')")
  @AuditLog(module = "industrial", action = "platform-match:update", targetType = "industrial_platform_item")
  public ApiResponse<IndustrialPlatformItemVO> updateMatch(@PathVariable Long id,
      @Valid @RequestBody IndustrialPlatformItemReq req) {
    return ApiResponse.ok(service.update("match", id, req));
  }

  @DeleteMapping("/platform-match/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-match:delete')")
  @AuditLog(module = "industrial", action = "platform-match:delete", targetType = "industrial_platform_item")
  public ApiResponse<Void> deleteMatch(@PathVariable Long id) {
    service.delete("match", id);
    return ApiResponse.ok();
  }

  @GetMapping("/platform-apprentice")
  @PreAuthorize("hasAuthority('industrial:platform-apprentice:view')")
  public ApiResponse<PageResponse<IndustrialPlatformItemVO>> pageApprentice(
      @Valid @ModelAttribute IndustrialPlatformQueryReq req) {
    return ApiResponse.ok(service.page("apprentice", req));
  }

  @GetMapping("/platform-apprentice/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-apprentice:view')")
  public ApiResponse<IndustrialPlatformItemVO> getApprentice(@PathVariable Long id) {
    return ApiResponse.ok(service.getById("apprentice", id));
  }

  @PostMapping("/platform-apprentice")
  @PreAuthorize("hasAuthority('industrial:platform-apprentice:create')")
  @AuditLog(module = "industrial", action = "platform-apprentice:create", targetType = "industrial_platform_item")
  public ApiResponse<IndustrialPlatformItemVO> createApprentice(@Valid @RequestBody IndustrialPlatformItemReq req) {
    return ApiResponse.ok(service.create("apprentice", req));
  }

  @PutMapping("/platform-apprentice/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-apprentice:update')")
  @AuditLog(module = "industrial", action = "platform-apprentice:update", targetType = "industrial_platform_item")
  public ApiResponse<IndustrialPlatformItemVO> updateApprentice(@PathVariable Long id,
      @Valid @RequestBody IndustrialPlatformItemReq req) {
    return ApiResponse.ok(service.update("apprentice", id, req));
  }

  @DeleteMapping("/platform-apprentice/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-apprentice:delete')")
  @AuditLog(module = "industrial", action = "platform-apprentice:delete", targetType = "industrial_platform_item")
  public ApiResponse<Void> deleteApprentice(@PathVariable Long id) {
    service.delete("apprentice", id);
    return ApiResponse.ok();
  }

  @GetMapping("/platform-comment")
  @PreAuthorize("hasAuthority('industrial:platform-comment:view')")
  public ApiResponse<PageResponse<IndustrialPlatformItemVO>> pageComment(
      @Valid @ModelAttribute IndustrialPlatformQueryReq req) {
    return ApiResponse.ok(service.page("comment", req));
  }

  @GetMapping("/platform-comment/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-comment:view')")
  public ApiResponse<IndustrialPlatformItemVO> getComment(@PathVariable Long id) {
    return ApiResponse.ok(service.getById("comment", id));
  }

  @PostMapping("/platform-comment")
  @PreAuthorize("hasAuthority('industrial:platform-comment:create')")
  @AuditLog(module = "industrial", action = "platform-comment:create", targetType = "industrial_platform_item")
  public ApiResponse<IndustrialPlatformItemVO> createComment(@Valid @RequestBody IndustrialPlatformItemReq req) {
    return ApiResponse.ok(service.create("comment", req));
  }

  @PutMapping("/platform-comment/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-comment:update')")
  @AuditLog(module = "industrial", action = "platform-comment:update", targetType = "industrial_platform_item")
  public ApiResponse<IndustrialPlatformItemVO> updateComment(@PathVariable Long id,
      @Valid @RequestBody IndustrialPlatformItemReq req) {
    return ApiResponse.ok(service.update("comment", id, req));
  }

  @DeleteMapping("/platform-comment/{id}")
  @PreAuthorize("hasAuthority('industrial:platform-comment:delete')")
  @AuditLog(module = "industrial", action = "platform-comment:delete", targetType = "industrial_platform_item")
  public ApiResponse<Void> deleteComment(@PathVariable Long id) {
    service.delete("comment", id);
    return ApiResponse.ok();
  }
}
