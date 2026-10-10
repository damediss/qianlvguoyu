package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionTheoryObjectiveScorerTest {

  private final CompetitionTheoryObjectiveScorer scorer =
      new CompetitionTheoryObjectiveScorer();

  @Test
  void awardsFullScoreOnlyForExactlyEqualMultipleChoiceSet() {
    BigDecimal points = BigDecimal.ONE;

    assertThat(scorer.scoreMultipleChoice(
        List.of("A", "C"), List.of("A", "C"), points)).isEqualByComparingTo("1");
    assertThat(scorer.scoreMultipleChoice(
        List.of("A", "C"), List.of("C", "A"), points)).isEqualByComparingTo("1");
    assertThat(scorer.scoreMultipleChoice(
        List.of("A", "C"), List.of("A"), points)).isEqualByComparingTo("0");
    assertThat(scorer.scoreMultipleChoice(
        List.of("A", "C"), List.of("A", "B"), points)).isEqualByComparingTo("0");
    assertThat(scorer.scoreMultipleChoice(
        List.of("A", "C"), List.of("A", "C", "D"), points)).isEqualByComparingTo("0");
    assertThat(scorer.scoreMultipleChoice(
        List.of("A", "C"), List.of(), points)).isEqualByComparingTo("0");
    assertThat(scorer.scoreMultipleChoice(
        List.of("A", "C"), null, points)).isEqualByComparingTo("0");
  }

  @Test
  void exactAnswerScoringSupportsSingleChoiceAndTrueFalse() {
    assertThat(scorer.scoreExactAnswer("A", " a ", BigDecimal.ONE))
        .isEqualByComparingTo("1");
    assertThat(scorer.scoreExactAnswer("TRUE", "false", new BigDecimal("0.5")))
        .isEqualByComparingTo("0");
  }
}
