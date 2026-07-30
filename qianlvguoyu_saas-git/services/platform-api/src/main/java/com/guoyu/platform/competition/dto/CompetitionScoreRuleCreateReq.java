package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CompetitionScoreRuleCreateReq(
    @NotNull Long stageId,
    @NotNull Long eventWorkTypeId,
    String scoringSystem,
    String theoryTemplate,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal theoryWeight,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal simulationPracticalWeight
) {
}
