package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionStageCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageDraftSaveReq;
import com.guoyu.platform.competition.dto.CompetitionStageQueryReq;
import com.guoyu.platform.competition.dto.CompetitionStageUpdateReq;
import com.guoyu.platform.competition.service.CompetitionStageApplicationService;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.competition.vo.CompetitionEventVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/competition")
public class CompetitionStageController {

  private final CompetitionStageApplicationService service;

  public CompetitionStageController(CompetitionStageApplicationService service) {
    this.service = service;
  }

  @GetMapping("/events/{eventId}/stages")
  @PreAuthorize("hasAuthority('competition:stage:view')")
  public ApiResponse<PageResponse<CompetitionStageVO>> page(
      @PathVariable Long eventId,
      @Valid @ModelAttribute CompetitionStageQueryReq req) {
    return ApiResponse.ok(service.page(eventId, req));
  }

  @PostMapping("/events/{eventId}/stages")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "stage:create", targetType = "competition_stage")
  public ApiResponse<CompetitionStageVO> create(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionStageCreateReq req) {
    return ApiResponse.ok(service.create(eventId, req));
  }

  @PutMapping("/events/{eventId}/stages/draft")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "stage:draft-save", targetType = "competition_stage")
  public ApiResponse<java.util.List<CompetitionStageVO>> saveDraft(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionStageDraftSaveReq req) {
    return ApiResponse.ok(service.saveDraft(eventId, req));
  }

  @PutMapping("/events/{eventId}/work-types/{eventWorkTypeId}/stages/draft")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "work-type-stage:draft-save", targetType = "competition_event_work_type_stage")
  public ApiResponse<java.util.List<CompetitionStageVO>> saveDraftForWorkType(
      @PathVariable Long eventId,
      @PathVariable Long eventWorkTypeId,
      @RequestParam(required = false) String eventWorkTypeLevelCode,
      @Valid @RequestBody CompetitionStageDraftSaveReq req) {
    return ApiResponse.ok(service.saveDraftForWorkType(
        eventId, eventWorkTypeId, eventWorkTypeLevelCode, req));
  }

  @PostMapping("/events/{eventId}/stages/confirm")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "stage:confirm", targetType = "competition_event")
  public ApiResponse<CompetitionEventVO> confirmStagePlan(@PathVariable Long eventId) {
    return ApiResponse.ok(service.confirmStagePlan(eventId));
  }

  @PutMapping("/stages/{stageId}")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "stage:update", targetType = "competition_stage")
  public ApiResponse<CompetitionStageVO> update(
      @PathVariable Long stageId,
      @Valid @RequestBody CompetitionStageUpdateReq req) {
    return ApiResponse.ok(service.update(stageId, req));
  }

  @DeleteMapping("/stages/{stageId}")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "stage:delete", targetType = "competition_stage")
  public ApiResponse<Void> delete(@PathVariable Long stageId) {
    service.delete(stageId);
    return ApiResponse.ok(null);
  }

  @PostMapping("/stages/{stageId}/configuration")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "stage:begin-configuration", targetType = "competition_stage")
  public ApiResponse<CompetitionStageVO> beginConfiguration(
      @PathVariable Long stageId, @RequestBody(required = false) String reason) {
    return ApiResponse.ok(service.beginConfiguration(stageId, reason));
  }

  @PostMapping("/stages/{stageId}/actions/lock-ready")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "stage:lock-ready", targetType = "competition_stage")
  public ApiResponse<CompetitionStageVO> lockReady(@PathVariable Long stageId) {
    return ApiResponse.ok(service.lockReady(stageId));
  }

  @PostMapping("/stages/{stageId}/actions/start")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "stage:start", targetType = "competition_stage")
  public ApiResponse<CompetitionStageVO> startStage(@PathVariable Long stageId) {
    return ApiResponse.ok(service.startStage(stageId));
  }

  @PostMapping("/stages/{stageId}/actions/close")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "stage:close", targetType = "competition_stage")
  public ApiResponse<CompetitionStageVO> closeStage(@PathVariable Long stageId) {
    return ApiResponse.ok(service.closeStage(stageId));
  }

  @PostMapping("/stages/{stageId}/actions/mark-settled")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "stage:mark-settled", targetType = "competition_stage")
  public ApiResponse<CompetitionStageVO> markSettled(@PathVariable Long stageId) {
    return ApiResponse.ok(service.markSettled(stageId));
  }

  @PostMapping("/stages/{stageId}/actions/publish-result")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "stage:publish-result", targetType = "competition_stage")
  public ApiResponse<CompetitionStageVO> publishResult(@PathVariable Long stageId) {
    return ApiResponse.ok(service.publishResult(stageId));
  }
}
