package com.guoyu.platform.competition.player.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionPlayerChangeMobileVerifyReq(
    @NotBlank @Size(max = 32) String idCardNo,
    @NotBlank @Size(max = 32) String newMobile) {
}
