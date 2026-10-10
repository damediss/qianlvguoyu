package com.guoyu.platform.competition.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.competition.service.CompetitionScoringReadinessApplicationService;
import com.guoyu.platform.competition.vo.CompetitionScoringReadinessVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/competition")
public class CompetitionScoringReadinessController {

  private final CompetitionScoringReadinessApplicationService service;

  public CompetitionScoringReadinessController(
      CompetitionScoringReadinessApplicationService service) {
    this.service = service;
  }

  @GetMapping("/events/{eventId}/scoring-readiness")
  @PreAuthorize("hasAuthority('competition:scoring-readiness:view')")
  public ApiResponse<CompetitionScoringReadinessVO> check(
      @PathVariable Long eventId,
      @RequestParam(required = false) Long stageId,
      @RequestParam(required = false) Long eventWorkTypeId,
      @RequestParam(required = false) Long eventWorkTypeLevelId) {
    return ApiResponse.ok(service.check(
        eventId, stageId, eventWorkTypeId, eventWorkTypeLevelId));
  }
}
