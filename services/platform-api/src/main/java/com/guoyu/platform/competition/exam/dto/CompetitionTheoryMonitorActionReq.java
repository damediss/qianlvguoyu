package com.guoyu.platform.competition.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionTheoryMonitorActionReq(
    @NotBlank @Size(max = 1000) String reason) {
}
