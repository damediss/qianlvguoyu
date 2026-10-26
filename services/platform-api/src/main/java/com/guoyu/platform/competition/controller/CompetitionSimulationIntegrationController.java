package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageQuery;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionSimulationProviderBindingReq;
import com.guoyu.platform.competition.dto.CompetitionSimulationSyncConfirmReq;
import com.guoyu.platform.competition.dto.CompetitionSimulationSyncCreateReq;
import com.guoyu.platform.competition.service.CompetitionSimulationIntegrationApplicationService;
import com.guoyu.platform.competition.integration.plantvr.PlantVrCompetitionClient.SessionInfo;
import com.guoyu.platform.competition.vo.CompetitionSimulationProviderBindingVO;
import com.guoyu.platform.competition.vo.CompetitionSimulationSyncBatchVO;
import com.guoyu.platform.competition.vo.CompetitionSimulationSyncRowVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@Validated
@RequestMapping("/competition")
public class CompetitionSimulationIntegrationController {
  private final CompetitionSimulationIntegrationApplicationService service;

  public CompetitionSimulationIntegrationController(
      CompetitionSimulationIntegrationApplicationService service) {
    this.service = service;
  }

  @GetMapping("/events/{eventId}/simulation-provider-binding")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ApiResponse<CompetitionSimulationProviderBindingVO> binding(
      @PathVariable Long eventId,
      @RequestParam @NotNull Long stageId,
      @RequestParam @NotNull Long eventWorkTypeId,
      @RequestParam(required = false) Long eventWorkTypeLevelId) {
    return ApiResponse.ok(service.binding(eventId, stageId, eventWorkTypeId,
        eventWorkTypeLevelId));
  }

  @GetMapping("/events/{eventId}/simulation-provider-sessions")
  @PreAuthorize("hasAuthority('competition:score:import')")
  public ApiResponse<List<SessionInfo>> availablePlantVrSessions(@PathVariable Long eventId) {
    return ApiResponse.ok(service.availablePlantVrSessions(eventId));
  }

  @PutMapping("/events/{eventId}/simulation-provider-binding")
  @PreAuthorize("hasAuthority('competition:score:import')")
  @AuditLog(module = "competition", action = "simulation-provider-binding:save",
      targetType = "competition_simulation_provider_binding", includeResult = true)
  public ApiResponse<CompetitionSimulationProviderBindingVO> saveBinding(
      @PathVariable Long eventId,
      @Valid @RequestBody CompetitionSimulationProviderBindingReq request) {
    return ApiResponse.ok(service.saveBinding(eventId, request));
  }

  @PostMapping("/simulation-score-syncs")
  @PreAuthorize("hasAuthority('competition:score:import')")
  @AuditLog(module = "competition", action = "simulation-score:sync",
      targetType = "competition_simulation_sync_batch", includeResult = true)
  public ResponseEntity<ApiResponse<CompetitionSimulationSyncBatchVO>> startSync(
      @Valid @RequestBody CompetitionSimulationSyncCreateReq request) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(ApiResponse.ok(service.startSync(request.bindingId())));
  }

  @GetMapping("/simulation-score-syncs/{batchId}")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ApiResponse<CompetitionSimulationSyncBatchVO> batch(@PathVariable Long batchId) {
    return ApiResponse.ok(service.batch(batchId));
  }

  @GetMapping("/simulation-score-syncs/{batchId}/rows")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ApiResponse<PageResponse<CompetitionSimulationSyncRowVO>> rows(
      @PathVariable Long batchId,
      @Valid @ModelAttribute PageQuery query) {
    return ApiResponse.ok(service.rows(batchId, query));
  }

  @PostMapping("/simulation-score-syncs/{batchId}/confirm")
  @PreAuthorize("hasAuthority('competition:score:import')")
  @AuditLog(module = "competition", action = "simulation-score:confirm",
      targetType = "competition_simulation_sync_batch", includeResult = true)
  public ResponseEntity<ApiResponse<CompetitionSimulationSyncBatchVO>> confirm(
      @PathVariable Long batchId,
      @Valid @RequestBody(required = false) CompetitionSimulationSyncConfirmReq request) {
    String correctionReason = request == null ? null : request.correctionReason();
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(ApiResponse.ok(service.confirm(batchId, correctionReason)));
  }

  @GetMapping("/score-sources/{scoreSourceId}/simulation-detail")
  @PreAuthorize("hasAuthority('competition:score:view')")
  public ApiResponse<CompetitionSimulationSyncRowVO> scoreDetail(
      @PathVariable Long scoreSourceId) {
    return ApiResponse.ok(service.scoreDetail(scoreSourceId));
  }
}
