package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CompetitionPracticalScoreItemReq(
    @NotNull Integer itemNo,
    @NotBlank String itemName,
    String criterion,
    @NotNull @DecimalMin("0.01") BigDecimal maxScore,
    @NotNull @DecimalMin("0") BigDecimal maxDeduction,
    @NotNull @DecimalMin("0.01") BigDecimal deductionStep
) {
}
