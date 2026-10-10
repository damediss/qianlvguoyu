package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompetitionEventOrgStatusUpdateReq(
    @NotBlank @Size(max = 32) String status
) {
}
