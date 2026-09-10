package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionEventCreateReq;
import com.guoyu.platform.competition.dto.CompetitionEventQueryReq;
import com.guoyu.platform.competition.dto.CompetitionEventUpdateReq;
import com.guoyu.platform.competition.service.CompetitionEventApplicationService;
import com.guoyu.platform.competition.vo.CompetitionEventVO;
import com.guoyu.platform.competition.vo.CompetitionEventPublishResultVO;
import com.guoyu.platform.competition.vo.CompetitionEventPublishReadinessVO;
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
@RequestMapping("/competition")
@Slf4j
public class CompetitionEventController {

  private final CompetitionEventApplicationService service;

  public CompetitionEventController(CompetitionEventApplicationService service) {
    this.service = service;
  }

  @PostMapping("/events")
  @PreAuthorize("hasAuthority('competition:event:create')")
  @AuditLog(module = "competition", action = "event:create", targetType = "competition_event")
  public ApiResponse<CompetitionEventVO> create(@Valid @RequestBody CompetitionEventCreateReq req) {
    return ApiResponse.ok(service.create(req));
  }

  @GetMapping("/events")
  @PreAuthorize("hasAuthority('competition:event:view')")
  public ApiResponse<PageResponse<CompetitionEventVO>> page(@Valid @ModelAttribute CompetitionEventQueryReq req) {
    return ApiResponse.ok(service.page(req));
  }

  @GetMapping("/events/{eventId}")
  @PreAuthorize("hasAuthority('competition:event:view')")
  public ApiResponse<CompetitionEventVO> getById(@PathVariable Long eventId) {
    return ApiResponse.ok(service.getById(eventId));
  }

  @PutMapping("/events/{eventId}")
  @PreAuthorize("hasAuthority('competition:event:update')")
  @AuditLog(module = "competition", action = "event:update", targetType = "competition_event")
  public ApiResponse<CompetitionEventVO> update(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionEventUpdateReq req) {
    return ApiResponse.ok(service.update(eventId, req));
  }

  @PostMapping("/events/{eventId}/publish")
  @PreAuthorize("hasAuthority('competition:event:publish')")
  @AuditLog(module = "competition", action = "event:publish", targetType = "competition_event", includeResult = true)
  public ApiResponse<CompetitionEventPublishResultVO> publish(@PathVariable Long eventId) {
    return ApiResponse.ok(service.publish(eventId));
  }

  @GetMapping("/events/{eventId}/publish-readiness")
  @PreAuthorize("hasAuthority('competition:event:publish')")
  public ApiResponse<CompetitionEventPublishReadinessVO> publishReadiness(
      @PathVariable Long eventId) {
    return ApiResponse.ok(service.publishReadiness(eventId));
  }
}
