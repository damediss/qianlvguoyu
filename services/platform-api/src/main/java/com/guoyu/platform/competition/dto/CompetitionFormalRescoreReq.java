package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionFormalRescoreReq(
    @NotBlank(message = "重新判分原因不能为空")
    @Size(max = 500, message = "重新判分原因不能超过500字") String reason) {
}
