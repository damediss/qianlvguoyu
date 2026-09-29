package com.guoyu.platform.competition.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PracticalTimeScoreCalculatorTest {
  private final PracticalTimeScoreCalculator calculator = new PracticalTimeScoreCalculator();

  @Test
  void timeScoringDisabledRecordsDurationWithoutBonusOrDeduction() {
    PracticalTimeScoreRule rule = rule(false, "STEP_DEDUCTION", 60L, "STEP_BONUS", 300L,
        "ALLOW_CONTINUE", 0L, bd("100"));
    PracticalTimeScoreCalculation result = calculate(rule, 5_100, bd("92"));
    assertEquals(5_100, result.effectiveDurationSeconds());
    assertEquals(bd("0"), result.timeBonus());
    assertEquals(bd("0"), result.timeDeduction());
    assertEquals(bd("92"), result.finalScore());
  }

  @Test
  void overtimeStepRoundsUpWhenThereIsAnyRemainder() {
    PracticalTimeScoreRule rule = rule(true, "STEP_DEDUCTION", 60L, "NONE", null,
        "ALLOW_CONTINUE", 0L, bd("100"));
    PracticalTimeScoreCalculation result = calculate(rule, 5_600, bd("100"));
    assertEquals(200, result.overtimeDurationSeconds());
    assertEquals(bd("4"), result.timeDeduction());
  }

  @Test
  void overtimeDeductionIsCappedAtConfiguredMaximum() {
    PracticalTimeScoreRule rule = new PracticalTimeScoreRule(5_400, true, "STEP_DEDUCTION",
        60L, bd("1"), bd("0"), bd("10"), "NONE", null, bd("0"), bd("0"), bd("0"),
        "ALLOW_CONTINUE", null, bd("100"));
    PracticalTimeScoreCalculation result = calculate(rule, 6_480, bd("100"));
    assertEquals(bd("10"), result.timeDeduction());
  }

  @Test
  void earlyFinishBonusUsesCompleteStepsOnly() {
    PracticalTimeScoreRule rule = rule(true, "NONE", null, "STEP_BONUS", 300L,
        "ALLOW_CONTINUE", 0L, bd("100"));
    PracticalTimeScoreCalculation result = calculate(rule, 4_680, bd("92"));
    assertEquals(720, result.earlyFinishDurationSeconds());
    assertEquals(bd("2"), result.timeBonus());
  }

  @Test
  void finalScoreIsCappedAtSchemeLimit() {
    PracticalTimeScoreRule rule = rule(true, "NONE", null, "FIXED_BONUS", null,
        "ALLOW_CONTINUE", 0L, bd("100"));
    rule = new PracticalTimeScoreRule(rule.standardDurationSeconds(), true, "NONE", null,
        bd("0"), bd("0"), bd("0"), "FIXED_BONUS", null, bd("0"), bd("5"), bd("5"),
        "ALLOW_CONTINUE", null, bd("100"));
    PracticalTimeScoreCalculation result = calculate(rule, 4_800, bd("98"));
    assertEquals(bd("100"), result.finalScore());
  }

  @Test
  void finalScoreNeverFallsBelowZero() {
    PracticalTimeScoreRule rule = rule(true, "FIXED_DEDUCTION", null, "NONE", null,
        "ALLOW_CONTINUE", 0L, bd("100"));
    rule = new PracticalTimeScoreRule(rule.standardDurationSeconds(), true, "FIXED_DEDUCTION",
        null, bd("0"), bd("10"), bd("10"), "NONE", null, bd("0"), bd("0"), bd("0"),
        "ALLOW_CONTINUE", null, bd("100"));
    PracticalTimeScoreCalculation result = calculate(rule, 5_401, bd("3"));
    assertEquals(bd("0"), result.finalScore());
  }

  @Test
  void pauseTimeIsSubtractedBeforeTimeScoring() {
    PracticalTimeScoreRule rule = rule(true, "STEP_DEDUCTION", 60L, "NONE", null,
        "ALLOW_CONTINUE", 0L, bd("100"));
    PracticalTimeScoreCalculation result = calculator.calculate(rule, Instant.EPOCH,
        Instant.EPOCH.plusSeconds(5_600), Duration.ofSeconds(200), bd("90"));
    assertEquals(5_400, result.effectiveDurationSeconds());
    assertEquals(0, result.overtimeDurationSeconds());
  }

  @Test
  void autoFinishThresholdIsReportedSeparatelyFromTimeScoring() {
    PracticalTimeScoreRule rule = rule(false, "NONE", null, "NONE", null,
        "AUTO_FINISH", 6_000L, bd("100"));
    PracticalTimeScoreCalculation result = calculate(rule, 6_000, bd("90"));
    assertTrue(result.autoFinishRequired());
    assertEquals(bd("0"), result.timeBonus());
    assertEquals(bd("0"), result.timeDeduction());
  }

  @Test
  void timeScoringDoesNotTriggerAutoFinishWhenContinueIsConfigured() {
    PracticalTimeScoreRule rule = rule(true, "NONE", null, "NONE", null,
        "ALLOW_CONTINUE", 6_000L, bd("100"));
    PracticalTimeScoreCalculation result = calculate(rule, 6_000, bd("90"));
    assertFalse(result.autoFinishRequired());
  }

  private PracticalTimeScoreCalculation calculate(
      PracticalTimeScoreRule rule, long elapsedSeconds, BigDecimal practicalScore) {
    return calculator.calculate(rule, Instant.EPOCH, Instant.EPOCH.plusSeconds(elapsedSeconds),
        Duration.ZERO, practicalScore);
  }

  private PracticalTimeScoreRule rule(boolean timeEnabled, String overtimeRule,
      Long overtimeStep, String earlyRule, Long earlyStep, String overtimeAction,
      Long maxDuration, BigDecimal scoreLimit) {
    return new PracticalTimeScoreRule(5_400, timeEnabled, overtimeRule, overtimeStep,
        bd("1"), bd("5"), bd("10"), earlyRule, earlyStep, bd("1"), bd("2"), bd("5"),
        overtimeAction, maxDuration == 0 ? null : maxDuration, scoreLimit);
  }

  private BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
