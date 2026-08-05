package com.guoyu.platform.competition.exam.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.exam.dto.CompetitionTheoryMonitorActionReq;
import com.guoyu.platform.competition.exam.service.CompetitionTheoryMonitorApplicationService;
import com.guoyu.platform.competition.exam.vo.CompetitionTheoryMonitorVO;
import com.guoyu.platform.competition.exam.vo.CompetitionTheoryResultSummaryVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition")
public class CompetitionTheoryMonitorController {
  private final CompetitionTheoryMonitorApplicationService service;

  public CompetitionTheoryMonitorController(CompetitionTheoryMonitorApplicationService service) {
    this.service = service;
  }

  @GetMapping("/theory-schedules/{versionId}/monitor")
  @PreAuthorize("hasAuthority('competition:theory-monitor:view')")
  public ApiResponse<List<CompetitionTheoryMonitorVO>> monitor(@PathVariable Long versionId) {
    return ApiResponse.ok(service.monitor(versionId));
  }

  @PostMapping("/theory-attempts/{attemptId}/force-offline")
  @PreAuthorize("hasAuthority('competition:theory-monitor:force-offline')")
  @AuditLog(module = "competition", action = "theory-monitor:force-offline",
      targetType = "competition_theory_attempt")
  public ApiResponse<Void> forceOffline(
      @PathVariable Long attemptId,
      @Valid @RequestBody CompetitionTheoryMonitorActionReq request) {
    service.forceOffline(attemptId, request.reason());
    return ApiResponse.ok();
  }

  @PostMapping("/theory-attempts/{attemptId}/force-submit")
  @PreAuthorize("hasAuthority('competition:theory-monitor:force-submit')")
  @AuditLog(module = "competition", action = "theory-monitor:force-submit",
      targetType = "competition_theory_attempt")
  public ApiResponse<Void> forceSubmit(
      @PathVariable Long attemptId,
      @Valid @RequestBody CompetitionTheoryMonitorActionReq request) {
    service.forceSubmit(attemptId, request.reason());
    return ApiResponse.ok();
  }

  @PostMapping("/theory-attempts/{attemptId}/restore-absence")
  @PreAuthorize("hasAuthority('competition:theory-monitor:restore-absence')")
  @AuditLog(module = "competition", action = "theory-monitor:restore-absence",
      targetType = "competition_theory_attempt")
  public ApiResponse<Void> restoreAbsence(
      @PathVariable Long attemptId,
      @Valid @RequestBody CompetitionTheoryMonitorActionReq request) {
    service.restoreAbsence(attemptId, request.reason());
    return ApiResponse.ok();
  }

  @GetMapping("/theory-schedules/{versionId}/results")
  @PreAuthorize("hasAuthority('competition:theory-result:view')")
  public ApiResponse<List<CompetitionTheoryResultSummaryVO>> results(@PathVariable Long versionId) {
    return ApiResponse.ok(service.results(versionId));
  }
}
