package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionTheoryAssignmentAdjustReq(
    @NotNull Long targetSessionRoomId,
    @NotBlank @Size(max = 1000) String reason
) {
}
