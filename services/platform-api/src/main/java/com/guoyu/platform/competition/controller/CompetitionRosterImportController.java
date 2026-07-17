package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.competition.dto.CompetitionRosterImportListQueryReq;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionRosterImportQueryReq;
import com.guoyu.platform.competition.dto.CompetitionRosterPrecheckReq;
import com.guoyu.platform.competition.service.CompetitionRosterImportApplicationService;
import com.guoyu.platform.competition.vo.CompetitionRosterImportDetailVO;
import com.guoyu.platform.competition.vo.CompetitionRosterImportVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition")
@Slf4j
public class CompetitionRosterImportController {

  private final CompetitionRosterImportApplicationService service;

  public CompetitionRosterImportController(CompetitionRosterImportApplicationService service) {
    this.service = service;
  }

  @PostMapping("/events/{eventId}/roster-imports/precheck")
  @PreAuthorize("hasAuthority('competition:roster:import')")
  @AuditLog(module = "competition", action = "roster-import:precheck", targetType = "competition_roster_import")
  public ApiResponse<CompetitionRosterImportDetailVO> precheck(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionRosterPrecheckReq req) {
    return ApiResponse.ok(service.precheck(eventId, req));
  }

  @GetMapping("/roster-imports/{batchId}")
  @PreAuthorize("hasAuthority('competition:roster:view')")
  public ApiResponse<CompetitionRosterImportDetailVO> getBatch(
      @PathVariable Long batchId,
      @Valid @ModelAttribute CompetitionRosterImportQueryReq req) {
    return ApiResponse.ok(service.getBatch(batchId, req));
  }

  @GetMapping("/events/{eventId}/roster-imports")
  @PreAuthorize("hasAuthority('competition:roster:view')")
  public ApiResponse<PageResponse<CompetitionRosterImportVO>> page(
      @PathVariable Long eventId,
      @Valid @ModelAttribute CompetitionRosterImportListQueryReq req) {
    return ApiResponse.ok(service.page(eventId, req));
  }

  @PostMapping("/roster-imports/{batchId}/confirm")
  @PreAuthorize("hasAuthority('competition:roster:confirm')")
  @AuditLog(module = "competition", action = "roster-import:confirm", targetType = "competition_roster_import")
  public ApiResponse<CompetitionRosterImportDetailVO> confirm(@PathVariable Long batchId) {
    return ApiResponse.ok(service.confirm(batchId));
  }
}
