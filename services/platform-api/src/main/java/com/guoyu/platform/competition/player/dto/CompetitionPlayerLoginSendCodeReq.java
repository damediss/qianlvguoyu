package com.guoyu.platform.competition.player.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionPlayerLoginSendCodeReq(@NotBlank @Size(max = 32) String mobile) {
}
