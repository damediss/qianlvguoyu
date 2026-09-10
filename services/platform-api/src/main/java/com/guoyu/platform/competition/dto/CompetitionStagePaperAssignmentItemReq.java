package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

public record CompetitionStagePaperAssignmentItemReq(
    @NotNull Long contestantProfileId,
    Long paperSnapshotId) {
}
