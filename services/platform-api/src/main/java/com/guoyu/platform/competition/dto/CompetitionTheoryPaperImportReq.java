package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

public record CompetitionTheoryPaperImportReq(
    @NotNull Long scoreRuleId,
    @NotNull Long courseId,
    @NotNull Long paperAId,
    @NotNull Long paperBId
) {
}
