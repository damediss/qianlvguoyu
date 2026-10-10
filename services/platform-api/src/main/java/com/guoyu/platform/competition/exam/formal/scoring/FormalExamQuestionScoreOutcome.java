package com.guoyu.platform.competition.exam.formal.scoring;

import java.math.BigDecimal;

public record FormalExamQuestionScoreOutcome(
    boolean answered,
    boolean correct,
    int answerVersion,
    BigDecimal maxScore,
    BigDecimal awardedScore) {

  public static FormalExamQuestionScoreOutcome unanswered(int answerVersion, BigDecimal maxScore) {
    return new FormalExamQuestionScoreOutcome(
        false, false, answerVersion, FormalExamScoringDecimals.money(maxScore),
        FormalExamScoringDecimals.zero());
  }

  public static FormalExamQuestionScoreOutcome scored(
      boolean correct, int answerVersion, BigDecimal maxScore, BigDecimal awardedScore) {
    return new FormalExamQuestionScoreOutcome(
        true, correct, answerVersion,
        FormalExamScoringDecimals.money(maxScore),
        FormalExamScoringDecimals.money(awardedScore));
  }
}
