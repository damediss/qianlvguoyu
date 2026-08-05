package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;

public record CompetitionStageRosterGenerateReq(
    @NotBlank String sourceType) {
}
