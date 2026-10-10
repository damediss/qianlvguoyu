package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionPlayerCreateReq;
import com.guoyu.platform.competition.dto.CompetitionPlayerQueryReq;
import com.guoyu.platform.competition.dto.CompetitionPlayerUpdateReq;
import com.guoyu.platform.competition.service.CompetitionPlayerManagementService;
import com.guoyu.platform.competition.vo.CompetitionPlayerVO;
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
@RequestMapping("/competition/players")
public class CompetitionPlayerManagementController {

  private final CompetitionPlayerManagementService service;

  public CompetitionPlayerManagementController(CompetitionPlayerManagementService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('competition:player:view')")
  public ApiResponse<PageResponse<CompetitionPlayerVO>> page(
      @Valid @ModelAttribute CompetitionPlayerQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/{registrationId}")
  @PreAuthorize("hasAuthority('competition:player:update')")
  public ApiResponse<CompetitionPlayerVO> getForEdit(@PathVariable Long registrationId) {
    return ApiResponse.ok(service.getForEdit(registrationId));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('competition:player:create')")
  @AuditLog(module = "competition", action = "player:create", targetType = "competition_registration")
  public ApiResponse<CompetitionPlayerVO> create(@Valid @RequestBody CompetitionPlayerCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @PutMapping("/{registrationId}")
  @PreAuthorize("hasAuthority('competition:player:update')")
  @AuditLog(module = "competition", action = "player:update", targetType = "competition_registration")
  public ApiResponse<CompetitionPlayerVO> update(
      @PathVariable Long registrationId,
      @Valid @RequestBody CompetitionPlayerUpdateReq req) {
    return ApiResponse.ok(service.update(registrationId, req));
  }

  @DeleteMapping("/{registrationId}")
  @PreAuthorize("hasAuthority('competition:player:delete')")
  @AuditLog(module = "competition", action = "player:delete", targetType = "competition_registration")
  public ApiResponse<Void> delete(@PathVariable Long registrationId) {
    service.delete(registrationId);
    return ApiResponse.ok(null);
  }
}
