package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.competition.domain.CompetitionPracticalAverageCalculator.PracticalAverageStatus;
import com.guoyu.platform.competition.domain.CompetitionPracticalAverageCalculator.RefereeScore;
import com.guoyu.platform.competition.enums.CompetitionPracticalScoreStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionPracticalAverageCalculatorTest {

  private final CompetitionPracticalAverageCalculator calculator =
      new CompetitionPracticalAverageCalculator();

  @Test
  void averagesExactlyThreeCurrentLockedScoresHalfUpToTwoDecimals() {
    var result = calculator.calculate(List.of(
        score(1L, "80", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true),
        score(2L, "80", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true),
        score(3L, "80.015", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true)));

    assertThat(result.status()).isEqualTo(PracticalAverageStatus.CONFIRMED);
    assertThat(result.score()).isEqualByComparingTo("80.01");
    assertThat(result.effectiveRefereeCount()).isEqualTo(3);
  }

  @Test
  void fewerOrMoreThanThreeEffectiveScoresRemainPending() {
    var two = calculator.calculate(List.of(
        score(1L, "80", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true),
        score(2L, "90", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true)));
    var four = calculator.calculate(List.of(
        score(1L, "80", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true),
        score(2L, "90", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true),
        score(3L, "70", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true),
        score(4L, "60", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true)));

    assertThat(two.confirmed()).isFalse();
    assertThat(two.score()).isNull();
    assertThat(four.confirmed()).isFalse();
    assertThat(four.score()).isNull();
  }

  @Test
  void ignoresRecusedVoidedUnlockedAndHistoricalScores() {
    var result = calculator.calculate(List.of(
        score(1L, "80", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true),
        score(2L, "90", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true),
        score(3L, "70", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, true),
        score(4L, "100", CompetitionPracticalScoreStatus.RECUSED, true),
        score(5L, "100", CompetitionPracticalScoreStatus.VOIDED, true),
        score(6L, "100", CompetitionPracticalScoreStatus.DRAFT, true),
        score(7L, "100", CompetitionPracticalScoreStatus.SUBMITTED_LOCKED, false)));

    assertThat(result.confirmed()).isTrue();
    assertThat(result.score()).isEqualByComparingTo("80.00");
  }

  private RefereeScore score(
      Long refereeId,
      String score,
      CompetitionPracticalScoreStatus status,
      boolean current) {
    return new RefereeScore(refereeId, new BigDecimal(score), status, current);
  }
}
