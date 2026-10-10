package com.guoyu.platform.competition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CompetitionRefereeAssignmentReplaceReq(
    @NotNull @Valid List<@NotNull Long> refereeProfileIds) {
}
