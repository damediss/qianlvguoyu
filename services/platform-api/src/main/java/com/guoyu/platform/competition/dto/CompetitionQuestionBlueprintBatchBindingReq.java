package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Binds a set of historical questions to one leaf assessment point. */
public record CompetitionQuestionBlueprintBatchBindingReq(
    @NotNull Long blueprintId,
    @NotNull Long blueprintNodeId,
    @NotEmpty List<@NotNull Long> questionIds
) {}
