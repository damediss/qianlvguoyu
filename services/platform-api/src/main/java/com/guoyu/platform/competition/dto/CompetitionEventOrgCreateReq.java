package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CompetitionEventOrgCreateReq(
    @NotNull Long orgId,
    @Min(0) Integer quota,
    @Min(0) @Max(1) Integer importPermission
) {
}
