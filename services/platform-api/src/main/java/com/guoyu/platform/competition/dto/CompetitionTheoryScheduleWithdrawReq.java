package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionTheoryScheduleWithdrawReq(
    @NotBlank @Size(max = 1000) String reason
) {
}
