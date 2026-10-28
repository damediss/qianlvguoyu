package com.guoyu.platform.exam.answercheck.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单题判定结果（方案 §10）。置信度按 5 位小数落库，但阈值比较使用未舍入原始值。
 */
public record ObjectiveAnswerCheckResult(
    String resultCode,
    boolean consistent,
    String originalAnswerKeys,
    String aiAnswerKeys,
    BigDecimal referenceConfidence,
    BigDecimal aiConfidence,
    BigDecimal minMargin,
    String decisionWarning,
    OptionResults optionResults) {

  public record OptionResults(
      String scope,
      String choice,
      Double confidence,
      Double top1Probability,
      Double top2Probability,
      Double margin,
      List<OptionLine> options) {
  }

  public record OptionLine(
      String optionId,
      String label,
      String text,
      boolean originalCorrect,
      Boolean aiCorrect,
      String choice,
      Double confidence,
      Double referenceProbability,
      Double top1Probability,
      Double top2Probability,
      Double margin) {
  }
}
