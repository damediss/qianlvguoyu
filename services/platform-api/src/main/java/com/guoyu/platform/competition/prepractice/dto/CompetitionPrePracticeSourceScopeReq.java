package com.guoyu.platform.competition.prepractice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CompetitionPrePracticeSourceScopeReq(
    @NotEmpty List<@NotNull Long> sourceWorkTypeIds,
    @NotEmpty List<@NotNull Integer> sourceLevelCodes) {
}
