package com.guoyu.platform.system.controller;

import lombok.extern.slf4j.Slf4j;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.system.dto.OrgCreateReq;
import com.guoyu.platform.system.dto.OrgUpdateReq;
import com.guoyu.platform.system.service.OrgApplicationService;
import com.guoyu.platform.system.vo.OrgSimpleVO;
import com.guoyu.platform.system.vo.OrgTreeVO;
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
@RequestMapping("/system/orgs")
@Slf4j
public class OrgController {

  private final OrgApplicationService orgApplicationService;

  public OrgController(OrgApplicationService orgApplicationService) {
    this.orgApplicationService = orgApplicationService;
  }

  @GetMapping("/tree")
  @PreAuthorize("hasAuthority('org:view')")
  public ApiResponse<List<OrgTreeVO>> tree() {
    return ApiResponse.ok(orgApplicationService.tree());
  }

  @GetMapping("/list")
  @PreAuthorize("hasAuthority('org:view')")
  public ApiResponse<List<OrgSimpleVO>> list() {
    return ApiResponse.ok(orgApplicationService.listSimple());
  }

  @PostMapping
  @PreAuthorize("hasAuthority('org:create')")
  @AuditLog(module = "system", action = "org:create", targetType = "org")
  public ApiResponse<OrgTreeVO> create(@Valid @RequestBody OrgCreateReq req) {
    return ApiResponse.ok(orgApplicationService.create(req));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('org:update')")
  @AuditLog(module = "system", action = "org:update", targetType = "org")
  public ApiResponse<OrgTreeVO> update(@PathVariable Long id, @Valid @RequestBody OrgUpdateReq req) {
    return ApiResponse.ok(orgApplicationService.update(id, req));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('org:delete')")
  @AuditLog(module = "system", action = "org:delete", targetType = "org")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    orgApplicationService.delete(id);
    return ApiResponse.ok();
  }
}
