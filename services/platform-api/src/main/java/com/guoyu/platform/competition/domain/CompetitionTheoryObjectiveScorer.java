package com.guoyu.platform.competition.domain;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class CompetitionTheoryObjectiveScorer {

  public BigDecimal scoreExactAnswer(
      String standardAnswer, String contestantAnswer, BigDecimal questionScore) {
    validateQuestionScore(questionScore);
    if (standardAnswer == null || standardAnswer.isBlank()) {
      throw new IllegalArgumentException("标准答案不能为空");
    }
    if (contestantAnswer == null) {
      return BigDecimal.ZERO;
    }
    return normalize(standardAnswer).equals(normalize(contestantAnswer))
        ? questionScore
        : BigDecimal.ZERO;
  }

  public BigDecimal scoreMultipleChoice(
      Collection<String> standardAnswers,
      Collection<String> contestantAnswers,
      BigDecimal questionScore) {
    validateQuestionScore(questionScore);
    Set<String> standard = normalizedSet(standardAnswers);
    if (standard.isEmpty()) {
      throw new IllegalArgumentException("多选题标准答案不能为空");
    }
    Set<String> submitted = normalizedSet(contestantAnswers);
    return standard.equals(submitted) ? questionScore : BigDecimal.ZERO;
  }

  private Set<String> normalizedSet(Collection<String> answers) {
    Set<String> normalized = new HashSet<>();
    if (answers == null) {
      return normalized;
    }
    for (String answer : answers) {
      if (answer != null && !answer.isBlank()) {
        normalized.add(normalize(answer));
      }
    }
    return normalized;
  }

  private String normalize(String answer) {
    return answer.trim().toUpperCase(Locale.ROOT);
  }

  private void validateQuestionScore(BigDecimal questionScore) {
    if (questionScore == null || questionScore.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("题目分值必须大于等于0");
    }
  }
}
