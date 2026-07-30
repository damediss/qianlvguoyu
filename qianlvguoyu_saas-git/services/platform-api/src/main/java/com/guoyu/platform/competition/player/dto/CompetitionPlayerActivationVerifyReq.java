package com.guoyu.platform.competition.player.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionPlayerActivationVerifyReq(
    @NotBlank @Size(max = 64) String entryCode,
    @NotBlank @Size(max = 32) String mobile,
    @NotNull Long challengeId,
    @NotBlank @Size(min = 6, max = 6) String code) {
}
