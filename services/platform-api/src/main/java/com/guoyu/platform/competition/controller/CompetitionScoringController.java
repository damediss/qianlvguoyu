package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionRankingSnapshotGenerateReq;
import com.guoyu.platform.competition.dto.CompetitionRankingPublishReq;
import com.guoyu.platform.competition.dto.CompetitionScoreSourceRevisionReq;
import com.guoyu.platform.competition.dto.CompetitionTotalScoreCalculateReq;
import com.guoyu.platform.competition.service.CompetitionScoringApplicationService;
import com.guoyu.platform.competition.vo.CompetitionRankingEntryVO;
import com.guoyu.platform.competition.vo.CompetitionRankingSnapshotDetailVO;
import com.guoyu.platform.competition.vo.CompetitionRankingSnapshotVO;
import com.guoyu.platform.competition.vo.CompetitionScoreSourceVO;
import com.guoyu.platform.competition.vo.CompetitionTotalScoreSnapshotVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition")
public class CompetitionScoringController {

  private final CompetitionScoringApplicationService service;

  public CompetitionScoringController(CompetitionScoringApplicationService service) {
    this.service = service;
  }

  @GetMapping("/events/{eventId}/score-sources")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ApiResponse<PageResponse<CompetitionScoreSourceVO>> listScoreSources(
      @PathVariable Long eventId,
      @RequestParam @NotNull Long stageId,
      @RequestParam @NotNull Long eventWorkTypeId,
      @RequestParam(required = false) Long eventWorkTypeLevelId,
      @RequestParam(required = false) Long contestantMemberId,
      @RequestParam(required = false)
      @Pattern(regexp = "theory|simulation|practical") String sourceType,
      @Valid @ModelAttribute PageQuery pageQuery) {
    return ApiResponse.ok(service.listScoreSources(
        eventId, stageId, eventWorkTypeId, eventWorkTypeLevelId, contestantMemberId, sourceType,
        pageQuery));
  }

  @PostMapping("/events/{eventId}/score-sources/revisions")
  @PreAuthorize("hasAuthority('competition:score:manage')")
  @AuditLog(
      module = "competition",
      action = "score-source:revise",
      targetType = "competition_score_source",
      includeResult = true)
  public ApiResponse<CompetitionScoreSourceVO> reviseScoreSource(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionScoreSourceRevisionReq request) {
    return ApiResponse.ok(service.reviseScoreSource(eventId, request));
  }

  @GetMapping("/events/{eventId}/total-score-snapshots")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ApiResponse<PageResponse<CompetitionTotalScoreSnapshotVO>> listTotalScoreSnapshots(
      @PathVariable Long eventId,
      @RequestParam @NotNull Long stageId,
      @RequestParam @NotNull Long eventWorkTypeId,
      @RequestParam(required = false) Long eventWorkTypeLevelId,
      @RequestParam(required = false) Long contestantMemberId,
      @RequestParam(required = false) Long scoreRuleId,
      @Valid @ModelAttribute PageQuery pageQuery) {
    return ApiResponse.ok(service.listTotalScoreSnapshots(
        eventId, stageId, eventWorkTypeId, eventWorkTypeLevelId, contestantMemberId, scoreRuleId,
        pageQuery));
  }

  @PostMapping("/events/{eventId}/total-score-snapshots/calculate")
  @PreAuthorize("hasAuthority('competition:score:calculate')")
  @AuditLog(
      module = "competition",
      action = "total-score:calculate",
      targetType = "competition_total_score_snapshot",
      includeResult = true)
  public ApiResponse<CompetitionTotalScoreSnapshotVO> calculateTotalScore(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionTotalScoreCalculateReq request) {
    return ApiResponse.ok(service.calculateTotalScore(eventId, request));
  }

  @GetMapping("/events/{eventId}/ranking-snapshots")
  @PreAuthorize("hasAuthority('competition:ranking:view')")
  public ApiResponse<PageResponse<CompetitionRankingSnapshotVO>> listRankingSnapshots(
      @PathVariable Long eventId,
      @RequestParam @NotNull Long stageId,
      @RequestParam @NotNull Long eventWorkTypeId,
      @RequestParam(required = false) Long eventWorkTypeLevelId,
      @RequestParam(required = false) Long scoreRuleId,
      @Valid @ModelAttribute PageQuery pageQuery) {
    return ApiResponse.ok(service.listRankingSnapshots(
        eventId, stageId, eventWorkTypeId, eventWorkTypeLevelId, scoreRuleId, pageQuery));
  }

  @GetMapping("/ranking-snapshots/{snapshotId}/entries")
  @PreAuthorize("hasAuthority('competition:ranking:view')")
  public ApiResponse<PageResponse<CompetitionRankingEntryVO>> listRankingEntries(
      @PathVariable Long snapshotId,
      @Valid @ModelAttribute PageQuery pageQuery) {
    return ApiResponse.ok(service.listRankingEntries(snapshotId, pageQuery));
  }

  @PostMapping("/events/{eventId}/ranking-snapshots/generate")
  @PreAuthorize("hasAuthority('competition:ranking:generate')")
  @AuditLog(
      module = "competition",
      action = "ranking-snapshot:generate",
      targetType = "competition_ranking_snapshot",
      includeResult = true)
  public ApiResponse<CompetitionRankingSnapshotDetailVO> generateRankingSnapshot(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionRankingSnapshotGenerateReq request) {
    return ApiResponse.ok(service.generateRankingSnapshot(eventId, request));
  }

  @PostMapping("/ranking-snapshots/{snapshotId}/confirm")
  @PreAuthorize("hasAuthority('competition:score:review')")
  @AuditLog(module = "competition", action = "ranking-snapshot:confirm",
      targetType = "competition_ranking_snapshot", includeResult = true)
  public ApiResponse<CompetitionRankingSnapshotVO> confirmRankingSnapshot(
      @PathVariable Long snapshotId) {
    return ApiResponse.ok(service.confirmRankingSnapshot(snapshotId));
  }

  @PostMapping("/ranking-snapshots/{snapshotId}/publish")
  @PreAuthorize("hasAuthority('competition:ranking:publish')")
  @AuditLog(module = "competition", action = "ranking-snapshot:publish",
      targetType = "competition_ranking_snapshot", includeResult = true)
  public ApiResponse<CompetitionRankingSnapshotVO> publishRankingSnapshot(
      @PathVariable Long snapshotId,
      @Valid @RequestBody(required = false) CompetitionRankingPublishReq request) {
    return ApiResponse.ok(service.publishRankingSnapshot(snapshotId,
        request == null ? null : request.correctionReason()));
  }
}
