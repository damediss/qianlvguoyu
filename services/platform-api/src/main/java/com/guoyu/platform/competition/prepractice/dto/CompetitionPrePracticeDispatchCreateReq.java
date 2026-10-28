package com.guoyu.platform.competition.prepractice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CompetitionPrePracticeDispatchCreateReq(
    @NotNull Long taskId,
    @NotNull Long eventId,
    @NotNull Long eventWorkTypeId,
    @NotBlank String levelCode,
    Long orgId,
    @NotNull Boolean selectAllMatching,
    @Size(max = 1000) List<@NotNull Long> contestantProfileIds) {
}
