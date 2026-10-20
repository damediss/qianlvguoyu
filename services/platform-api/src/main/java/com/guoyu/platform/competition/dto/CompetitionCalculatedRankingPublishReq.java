package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionCalculatedRankingPublishReq(
    @NotNull Long stageId,
    @NotNull Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    @Size(max = 500) String correctionReason) {
}
