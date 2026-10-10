package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionStageSettlementCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageSettlementPublishReq;
import com.guoyu.platform.competition.dto.CompetitionStageFinalPodiumReq;
import com.guoyu.platform.competition.service.CompetitionStageSettlementApplicationService;
import com.guoyu.platform.competition.vo.CompetitionStageSettlementPreflightVO;
import com.guoyu.platform.competition.vo.CompetitionStageSettlementVO;
import com.guoyu.platform.competition.vo.CompetitionStageReferenceSnapshotVO;
import com.guoyu.platform.competition.vo.CompetitionStageFinalPodiumVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition")
public class CompetitionStageSettlementController {
  private final CompetitionStageSettlementApplicationService service;
  public CompetitionStageSettlementController(CompetitionStageSettlementApplicationService service) { this.service = service; }

  @GetMapping("/execution-plans/{planId}/settlement-preflight")
  @PreAuthorize("hasAuthority('competition:settlement:view')")
  public ApiResponse<CompetitionStageSettlementPreflightVO> preflight(@PathVariable Long planId, @RequestParam Long scoreRuleId, @RequestParam(required = false) List<Long> absentContestantMemberIds) {
    return ApiResponse.ok(service.preflight(planId, scoreRuleId, absentContestantMemberIds));
  }

  @PostMapping("/execution-plans/{planId}/settlements")
  @PreAuthorize("hasAuthority('competition:settlement:manage')")
  @AuditLog(module = "competition", action = "settlement:create", targetType = "competition_stage_settlement")
  public ApiResponse<CompetitionStageSettlementVO> create(@PathVariable Long planId, @Valid @RequestBody CompetitionStageSettlementCreateReq req) {
    return ApiResponse.ok(service.create(planId, req));
  }

  @GetMapping("/stage-settlements/{settlementId}")
  @PreAuthorize("hasAuthority('competition:settlement:view')")
  public ApiResponse<CompetitionStageSettlementVO> get(@PathVariable Long settlementId) { return ApiResponse.ok(service.get(settlementId)); }

  @GetMapping("/execution-plans/{planId}/settlements")
  @PreAuthorize("hasAuthority('competition:settlement:view')")
  public ApiResponse<List<CompetitionStageSettlementVO>> list(@PathVariable Long planId) { return ApiResponse.ok(service.list(planId)); }

  @PostMapping("/stage-settlements/{settlementId}/publish")
  @PreAuthorize("hasAuthority('competition:settlement:publish')")
  @AuditLog(module = "competition", action = "settlement:publish", targetType = "competition_stage_settlement")
  public ApiResponse<CompetitionStageSettlementVO> publish(@PathVariable Long settlementId, @Valid @RequestBody(required = false) CompetitionStageSettlementPublishReq req) { return ApiResponse.ok(service.publish(settlementId, req)); }

  @GetMapping("/stage-settlements/{settlementId}/promotion-reference")
  @PreAuthorize("hasAuthority('competition:settlement:view')")
  public ApiResponse<CompetitionStageReferenceSnapshotVO> reference(@PathVariable Long settlementId) {
    return ApiResponse.ok(service.getReference(settlementId));
  }

  @PostMapping("/stage-settlements/{settlementId}/promotion-reference/generate")
  @PreAuthorize("hasAuthority('competition:settlement:manage')")
  @AuditLog(module = "competition", action = "promotion-reference:generate", targetType = "competition_stage_reference_snapshot")
  public ApiResponse<CompetitionStageReferenceSnapshotVO> generateReference(@PathVariable Long settlementId) {
    return ApiResponse.ok(service.createReference(settlementId));
  }

  @PostMapping("/stage-settlements/{settlementId}/promotion-reference/confirm")
  @PreAuthorize("hasAuthority('competition:settlement:publish')")
  @AuditLog(module = "competition", action = "promotion-reference:confirm", targetType = "competition_stage_reference_snapshot")
  public ApiResponse<CompetitionStageReferenceSnapshotVO> confirmReference(@PathVariable Long settlementId) {
    return ApiResponse.ok(service.confirmReference(settlementId));
  }

  @GetMapping("/stage-settlements/{settlementId}/final-podium")
  @PreAuthorize("hasAuthority('competition:settlement:view')")
  public ApiResponse<CompetitionStageFinalPodiumVO> finalPodium(@PathVariable Long settlementId) {
    return ApiResponse.ok(service.getFinalPodium(settlementId));
  }

  @PutMapping("/stage-settlements/{settlementId}/final-podium")
  @PreAuthorize("hasAuthority('competition:settlement:manage')")
  @AuditLog(module = "competition", action = "final-podium:save", targetType = "competition_stage_final_podium")
  public ApiResponse<CompetitionStageFinalPodiumVO> saveFinalPodium(
      @PathVariable Long settlementId, @Valid @RequestBody CompetitionStageFinalPodiumReq req) {
    return ApiResponse.ok(service.saveFinalPodium(settlementId, req));
  }

  @PostMapping("/stage-settlements/{settlementId}/final-podium/confirm")
  @PreAuthorize("hasAuthority('competition:settlement:publish')")
  @AuditLog(module = "competition", action = "final-podium:confirm", targetType = "competition_stage_final_podium")
  public ApiResponse<CompetitionStageFinalPodiumVO> confirmFinalPodium(@PathVariable Long settlementId) {
    return ApiResponse.ok(service.confirmFinalPodium(settlementId));
  }
}
