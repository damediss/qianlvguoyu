package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

public record CompetitionTheoryScheduleCreateReq(
    @NotNull Long stageId,
    @NotNull Long eventWorkTypeId,
    @NotNull Long scoreRuleId
) {
}
