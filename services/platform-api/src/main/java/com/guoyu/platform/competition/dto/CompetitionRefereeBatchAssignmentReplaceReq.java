package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Replaces the same referee set for several practical-paper contestants. */
public record CompetitionRefereeBatchAssignmentReplaceReq(
    @NotEmpty @Valid List<@NotNull Long> contestantMemberIds,
    @NotNull @Valid List<@NotNull Long> refereeProfileIds) {
}
