package com.guoyu.platform.competition.prepractice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CompetitionPrePracticeQuestionCountReq(
    @NotNull @Valid CompetitionPrePracticeSourceScopeReq sourceScope) {
}
