package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record CompetitionScoreRuleUpdateReq(
    String scoringSystem,
    String theoryTemplate,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal theoryWeight,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal simulationPracticalWeight
) {
}
