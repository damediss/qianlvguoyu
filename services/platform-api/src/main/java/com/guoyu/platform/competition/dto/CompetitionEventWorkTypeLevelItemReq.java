package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompetitionEventWorkTypeLevelItemReq(
    Long id,
    @NotBlank @Pattern(regexp = "[1-5]") String levelCode,
    @NotBlank @Size(max = 64) String levelName,
    @Size(max = 64) String sourceLevelCode,
    @Size(max = 255) String sourceLevelText,
    @Pattern(regexp = "matched|unresolved|confirmed") String reconciliationStatus
) {
}
