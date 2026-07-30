package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.audit.AuditLog;
import com.guoyu.platform.competition.dto.CompetitionStageCreateReq;
import com.guoyu.platform.competition.dto.CompetitionStageQueryReq;
import com.guoyu.platform.competition.dto.CompetitionStageUpdateReq;
import com.guoyu.platform.competition.service.CompetitionStageApplicationService;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import jakarta.validation.Valid;
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

  @PutMapping("/stages/{stageId}")
  @PreAuthorize("hasAuthority('competition:stage:manage')")
  @AuditLog(module = "competition", action = "stage:update", targetType = "competition_stage")
  public ApiResponse<CompetitionStageVO> update(
      @PathVariable Long stageId,
      @Valid @RequestBody CompetitionStageUpdateReq req) {
    return ApiResponse.ok(service.update(stageId, req));
  }
}
