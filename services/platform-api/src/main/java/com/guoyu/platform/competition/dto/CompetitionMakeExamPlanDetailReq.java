package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CompetitionMakeExamPlanDetailReq(
    @NotBlank String questionType,
    @NotNull Integer questionCount,
    @NotNull @DecimalMin("0.01") BigDecimal point,
    Integer difficultyStart,
    Integer difficultyEnd,
    @NotNull Integer sortOrder
) {
}
