package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionContestantWorkTypeConfirmReq(
    @NotNull Long eventWorkTypeId,
    @Size(max = 500) String note
) {
}
