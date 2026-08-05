package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CompetitionScoreRuleCreateReq(
    @NotNull Long stageId,
    @NotNull Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    String scoringSystem,
    String theoryTemplate,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal theoryWeight,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal simulationWeight,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal practicalWeight,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal simulationPracticalWeight
) {

  public CompetitionScoreRuleCreateReq(
      Long stageId,
      Long eventWorkTypeId,
      String scoringSystem,
      String theoryTemplate,
      BigDecimal theoryWeight,
      BigDecimal simulationPracticalWeight) {
    this(stageId, eventWorkTypeId, null, scoringSystem, theoryTemplate, theoryWeight,
        null, null, simulationPracticalWeight);
  }
}
