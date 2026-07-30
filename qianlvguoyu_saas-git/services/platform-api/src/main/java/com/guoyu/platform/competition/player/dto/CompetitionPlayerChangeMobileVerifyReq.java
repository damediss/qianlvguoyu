package com.guoyu.platform.competition.player.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionPlayerChangeMobileVerifyReq(
    @NotBlank @Size(max = 32) String newMobile,
    @NotNull Long newChallengeId,
    @NotBlank @Size(min = 6, max = 6) String newCode) {
}
