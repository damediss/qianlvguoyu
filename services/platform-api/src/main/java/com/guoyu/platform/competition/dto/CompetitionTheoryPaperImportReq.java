package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

public record CompetitionTheoryPaperImportReq(
    @NotNull Long stageId,
    @NotNull Long eventWorkTypeId,
    @NotNull Long courseId,
    @NotNull Long paperAId,
    @NotNull Long paperBId
) {
}
