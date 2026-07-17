package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionEventOrgCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventOrgQueryReq;
import com.guoyu.platform.competition.dto.CompetitionEventOrgStatusUpdateReq;
import com.guoyu.platform.competition.service.CompetitionEventOrgApplicationService;
import com.guoyu.platform.competition.vo.CompetitionEventOrgVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/competition/events/{eventId}/organizations")
@Slf4j
public class CompetitionEventOrgController {

  private final CompetitionEventOrgApplicationService service;

  public CompetitionEventOrgController(CompetitionEventOrgApplicationService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('competition:event:view') and hasAuthority('competition:event-org:view')")
  public ApiResponse<PageResponse<CompetitionEventOrgVO>> page(
      @PathVariable Long eventId,
      @Valid @ModelAttribute CompetitionEventOrgQueryReq req) {
    return ApiResponse.ok(service.page(eventId, req));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('competition:event-org:manage')")
  @AuditLog(module = "competition", action = "event-org:create", targetType = "competition_event_org")
  public ApiResponse<CompetitionEventOrgVO> create(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionEventOrgCreateReq req) {
    return ApiResponse.ok(service.create(eventId, req));
  }

  @PutMapping("/{orgId}")
  @PreAuthorize("hasAuthority('competition:event-org:manage')")
  @AuditLog(module = "competition", action = "event-org:update", targetType = "competition_event_org")
  public ApiResponse<CompetitionEventOrgVO> updateStatus(
      @PathVariable Long eventId,
      @PathVariable Long orgId,
      @Valid @RequestBody CompetitionEventOrgStatusUpdateReq req) {
    return ApiResponse.ok(service.updateStatus(eventId, orgId, req));
  }
}
