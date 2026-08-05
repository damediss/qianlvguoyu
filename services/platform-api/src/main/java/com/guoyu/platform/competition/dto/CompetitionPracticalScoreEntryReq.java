package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CompetitionPracticalScoreEntryReq(
    @NotNull Long ruleItemId,
    @NotNull @DecimalMin("0.00") BigDecimal score,
    @NotNull @DecimalMin("0.00") BigDecimal deduction,
    String remark) { }
