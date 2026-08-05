package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CompetitionScoringSystemBCalculatorLevelTest {

  @Test
  void calculatesIndependentWeightsWithPublishedPrecision() {
    var calculator = new CompetitionScoringSystemBCalculator();
    var result = calculator.calculateLevelScoped(
        new CompetitionScoringSystemBCalculator.ScoreInput(new BigDecimal("85"), true),
        new CompetitionScoringSystemBCalculator.ScoreInput(new BigDecimal("90"), true),
        new CompetitionScoringSystemBCalculator.ScoreInput(new BigDecimal("80"), true),
        new BigDecimal("50.00"), new BigDecimal("20.00"), new BigDecimal("30.00"));

    assertThat(result.totalScore()).isEqualByComparingTo("84.50");
    assertThat(result.simulationPracticalScore()).isEqualByComparingTo("85.00");
  }
}
