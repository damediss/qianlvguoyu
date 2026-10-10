package com.guoyu.platform.competition.player.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionPlayerActivationSendReq(
    @NotBlank @Size(max = 64) String entryCode,
    @NotBlank @Size(max = 32) String mobile) {
}
