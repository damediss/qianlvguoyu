package com.guoyu.platform.competition.player.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionPlayerChangeMobileSendNewCodeReq(
    @NotNull Long oldChallengeId,
    @NotBlank @Size(min = 6, max = 6) String oldCode,
    @NotBlank @Size(max = 32) String newMobile) {
}
