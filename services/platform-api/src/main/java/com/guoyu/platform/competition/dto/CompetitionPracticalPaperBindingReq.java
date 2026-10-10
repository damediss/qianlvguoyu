package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

/** Binds a published practical make-exam snapshot to a stage scope. */
public record CompetitionPracticalPaperBindingReq(
    @NotNull Long eventWorkTypeId,
    @NotNull Long eventWorkTypeLevelId,
    @NotNull Long paperSnapshotId) {
}
