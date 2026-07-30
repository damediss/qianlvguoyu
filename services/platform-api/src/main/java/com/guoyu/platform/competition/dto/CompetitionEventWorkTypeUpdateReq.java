package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompetitionEventWorkTypeUpdateReq(
    @NotNull Long workTypeId,
    @Size(max = 255) String displayName,
    @Size(max = 32) String status,
    @Min(0) Integer sortOrder
) {
}
