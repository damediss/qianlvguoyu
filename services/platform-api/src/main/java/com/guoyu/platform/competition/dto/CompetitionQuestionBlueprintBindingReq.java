package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

public record CompetitionQuestionBlueprintBindingReq(
    @NotNull Long blueprintId,
    @NotNull Long blueprintNodeId
) {
}
