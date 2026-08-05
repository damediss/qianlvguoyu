package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionStageExecutionPlanCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStagePlanResourceBatchReq;
import com.guoyu.platform.competition.dto.CompetitionStageRosterAdjustmentReq;
import com.guoyu.platform.competition.dto.CompetitionStageRosterGenerateReq;
import com.guoyu.platform.competition.service.CompetitionStageExecutionPlanApplicationService;
import com.guoyu.platform.competition.vo.CompetitionStageExecutionPlanVO;
import com.guoyu.platform.competition.vo.CompetitionStagePlanPreflightVO;
import com.guoyu.platform.competition.vo.CompetitionStageRosterItemVO;
import com.guoyu.platform.competition.vo.CompetitionStageRosterSnapshotVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition")
public class CompetitionStageExecutionPlanController {
  private final CompetitionStageExecutionPlanApplicationService service;

  public CompetitionStageExecutionPlanController(CompetitionStageExecutionPlanApplicationService service) {
    this.service = service;
  }

  @GetMapping("/stages/{stageId}/execution-plans")
  @PreAuthorize("hasAuthority('competition:stage-plan:view')")
  public ApiResponse<List<CompetitionStageExecutionPlanVO>> list(
      @PathVariable Long stageId,
      @RequestParam @NotNull Long eventWorkTypeId,
      @RequestParam(required = false) Long eventWorkTypeLevelId) {
    return ApiResponse.ok(service.list(stageId, eventWorkTypeId, eventWorkTypeLevelId));
  }

  @PostMapping("/stages/{stageId}/execution-plans")
  @PreAuthorize("hasAuthority('competition:stage-plan:manage')")
  @AuditLog(module = "competition", action = "stage-plan:create", targetType = "competition_stage_execution_plan")
  public ApiResponse<CompetitionStageExecutionPlanVO> create(
      @PathVariable Long stageId, @Valid @RequestBody CompetitionStageExecutionPlanCreateReq req) {
    return ApiResponse.ok(service.create(stageId, req));
  }

  @GetMapping("/execution-plans/{planId}")
  @PreAuthorize("hasAuthority('competition:stage-plan:view')")
  public ApiResponse<CompetitionStageExecutionPlanVO> get(@PathVariable Long planId) {
    return ApiResponse.ok(service.get(planId));
  }

  @PutMapping("/execution-plans/{planId}/resources")
  @PreAuthorize("hasAuthority('competition:stage-plan:manage')")
  @AuditLog(module = "competition", action = "stage-plan:resources", targetType = "competition_stage_execution_plan")
  public ApiResponse<CompetitionStageExecutionPlanVO> resources(
      @PathVariable Long planId, @Valid @RequestBody CompetitionStagePlanResourceBatchReq req) {
    return ApiResponse.ok(service.replaceResources(planId, req));
  }

  @GetMapping("/execution-plans/{planId}/preflight")
  @PreAuthorize("hasAuthority('competition:stage-plan:view')")
  public ApiResponse<CompetitionStagePlanPreflightVO> preflight(@PathVariable Long planId) {
    return ApiResponse.ok(service.preflight(planId));
  }

  @PostMapping("/execution-plans/{planId}/publish")
  @PreAuthorize("hasAuthority('competition:stage-plan:publish')")
  @AuditLog(module = "competition", action = "stage-plan:publish", targetType = "competition_stage_execution_plan")
  public ApiResponse<CompetitionStageExecutionPlanVO> publish(@PathVariable Long planId) {
    return ApiResponse.ok(service.publish(planId));
  }

  @PostMapping("/execution-plans/{planId}/withdraw")
  @PreAuthorize("hasAuthority('competition:stage-plan:exception')")
  @AuditLog(module = "competition", action = "stage-plan:withdraw", targetType = "competition_stage_execution_plan")
  public ApiResponse<CompetitionStageExecutionPlanVO> withdraw(
      @PathVariable Long planId, @RequestBody(required = false) String reason) {
    return ApiResponse.ok(service.withdraw(planId, reason));
  }

  @PostMapping("/execution-plans/{planId}/roster/generate")
  @PreAuthorize("hasAuthority('competition:stage-roster:manage')")
  @AuditLog(module = "competition", action = "stage-roster:generate", targetType = "competition_stage_roster_snapshot")
  public ApiResponse<CompetitionStageRosterSnapshotVO> generateRoster(
      @PathVariable Long planId, @Valid @RequestBody CompetitionStageRosterGenerateReq req) {
    return ApiResponse.ok(service.generateRoster(planId, req));
  }

  @GetMapping("/execution-plans/{planId}/roster")
  @PreAuthorize("hasAuthority('competition:stage-plan:view')")
  public ApiResponse<List<CompetitionStageRosterSnapshotVO>> roster(@PathVariable Long planId) {
    return ApiResponse.ok(service.rosterHistory(planId));
  }

  @PostMapping("/execution-plans/{planId}/roster/{snapshotId}/publish")
  @PreAuthorize("hasAuthority('competition:stage-roster:manage')")
  @AuditLog(module = "competition", action = "stage-roster:publish", targetType = "competition_stage_roster_snapshot")
  public ApiResponse<CompetitionStageRosterSnapshotVO> publishRoster(
      @PathVariable Long planId, @PathVariable Long snapshotId) {
    return ApiResponse.ok(service.publishRoster(planId, snapshotId));
  }

  @PostMapping("/execution-plans/{planId}/roster/{snapshotId}/adjust")
  @PreAuthorize("hasAuthority('competition:stage-roster:manage')")
  @AuditLog(module = "competition", action = "stage-roster:adjust", targetType = "competition_stage_roster_item")
  public ApiResponse<CompetitionStageRosterItemVO> adjustRoster(
      @PathVariable Long planId, @PathVariable Long snapshotId,
      @Valid @RequestBody CompetitionStageRosterAdjustmentReq req) {
    return ApiResponse.ok(service.adjustRoster(planId, snapshotId, req));
  }
}
