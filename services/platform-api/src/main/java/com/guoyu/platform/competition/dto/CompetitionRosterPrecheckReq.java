package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

public record CompetitionRosterPrecheckReq(
    @NotNull Long fileId,
    @NotNull Long orgId
) {
}
