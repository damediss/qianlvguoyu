package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;

public record CompetitionPracticalScoreUnlockReq(@NotBlank String reason) { }
