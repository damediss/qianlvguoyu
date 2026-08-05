package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionStageExecutionPlanCreateReq(
    @NotNull Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    @Size(max = 128) String resourceVersion,
    Long scoreRuleId,
    Integer promotionQuota,
    String promotionRuleType,
    Integer promotionPercentage) {

  public CompetitionStageExecutionPlanCreateReq(
      Long eventWorkTypeId, Long eventWorkTypeLevelId, String resourceVersion) {
    this(eventWorkTypeId, eventWorkTypeLevelId, resourceVersion, null, null, null, null);
  }
}
