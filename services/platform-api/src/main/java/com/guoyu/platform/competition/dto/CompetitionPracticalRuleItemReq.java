package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CompetitionPracticalRuleItemReq(
    @NotBlank String itemCode,
    @NotBlank String itemName,
    @NotNull @DecimalMin("0.01") BigDecimal maxScore,
    Boolean deductionAllowed,
    String criteriaPayload) { }
