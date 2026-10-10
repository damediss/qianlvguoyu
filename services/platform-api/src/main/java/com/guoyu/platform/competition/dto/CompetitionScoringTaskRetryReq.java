package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionScoringTaskRetryReq(
    @NotBlank(message = "重试原因不能为空")
    @Size(max = 500, message = "重试原因不能超过500字") String reason) {
}
