package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;

/** Binds a published practical competition paper snapshot to a draft execution session. */
public record CompetitionPracticalPaperSnapshotBindReq(
    @NotNull Long paperSnapshotId) {
}
