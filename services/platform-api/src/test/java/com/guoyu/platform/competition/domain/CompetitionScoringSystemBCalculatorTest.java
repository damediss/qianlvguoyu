package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.competition.domain.CompetitionScoringSystemBCalculator.ScoreInput;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CompetitionScoringSystemBCalculatorTest {

  private final CompetitionScoringSystemBCalculator calculator =
      new CompetitionScoringSystemBCalculator();

  @Test
  void calculatesFrozenDocumentExample() {
    var result = calculator.calculate(
        confirmed("85"),
        confirmed("90"),
        confirmed("80"),
        new BigDecimal("40"),
        new BigDecimal("60"));

    assertThat(result.confirmed()).isTrue();
    assertThat(result.simulationPracticalScore()).isEqualByComparingTo("85");
    assertThat(result.totalScore()).isEqualByComparingTo("85");
  }

  @Test
  void unconfirmedSourceCannotProduceConfirmedTotal() {
    var result = calculator.calculate(
        confirmed("85"),
        new ScoreInput(new BigDecimal("90"), false),
        confirmed("80"),
        new BigDecimal("40"),
        new BigDecimal("60"));

    assertThat(result.confirmed()).isFalse();
    assertThat(result.totalScore()).isNull();
  }

  @Test
  void rejectsInvalidWeightsAndOutOfRangeScores() {
    assertThatThrownBy(() -> calculator.calculate(
        confirmed("85"), confirmed("90"), confirmed("80"),
        new BigDecimal("40"), new BigDecimal("50")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> calculator.calculate(
        confirmed("-1"), confirmed("90"), confirmed("80"),
        new BigDecimal("40"), new BigDecimal("60")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> calculator.calculate(
        confirmed("85"), confirmed("101"), confirmed("80"),
        new BigDecimal("40"), new BigDecimal("60")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private ScoreInput confirmed(String score) {
    return new ScoreInput(new BigDecimal(score), true);
  }
}
