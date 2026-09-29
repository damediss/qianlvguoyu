package com.guoyu.platform.competition.domain;

import java.math.BigDecimal;
import java.time.Duration;

public record PracticalTimeScoreCalculation(
    long elapsedDurationSeconds,
    long pausedDurationSeconds,
    long effectiveDurationSeconds,
    long overtimeDurationSeconds,
    long earlyFinishDurationSeconds,
    BigDecimal timeBonus,
    BigDecimal timeDeduction,
    BigDecimal practicalScore,
    BigDecimal finalScore,
    boolean autoFinishRequired,
    String calculationDetail) {

  public Duration effectiveDuration() {
    return Duration.ofSeconds(effectiveDurationSeconds);
  }

  public Duration elapsedDuration() {
    return Duration.ofSeconds(elapsedDurationSeconds);
  }

  public Duration pausedDuration() {
    return Duration.ofSeconds(pausedDurationSeconds);
  }

  public Duration overtimeDuration() {
    return Duration.ofSeconds(overtimeDurationSeconds);
  }

  public Duration earlyFinishDuration() {
    return Duration.ofSeconds(earlyFinishDurationSeconds);
  }
}
