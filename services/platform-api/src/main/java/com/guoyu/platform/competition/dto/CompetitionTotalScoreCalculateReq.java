package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

public record CompetitionTotalScoreCalculateReq(
    @NotNull Long stageId,
    @NotNull Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    @NotNull Long contestantMemberId,
    @NotNull Long scoreRuleId) {

  public CompetitionTotalScoreCalculateReq(
      Long stageId, Long eventWorkTypeId, Long contestantMemberId, Long scoreRuleId) {
    this(stageId, eventWorkTypeId, null, contestantMemberId, scoreRuleId);
  }
}
