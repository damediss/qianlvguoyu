package com.guoyu.platform.competition.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Server-side time scoring. It never uses a device clock or floating point arithmetic. */
@Component
public class PracticalTimeScoreCalculator {
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  public PracticalTimeScoreCalculation calculate(
      PracticalTimeScoreRule rule,
      Instant startTime,
      Instant finishTime,
      Duration pausedDuration,
      BigDecimal practicalScore) {
    Objects.requireNonNull(rule, "rule is required");
    Objects.requireNonNull(startTime, "startTime is required");
    Objects.requireNonNull(finishTime, "finishTime is required");
    Objects.requireNonNull(practicalScore, "practicalScore is required");
    if (finishTime.isBefore(startTime)) {
      throw new IllegalArgumentException("finishTime must not be before startTime");
    }
    if (practicalScore.signum() < 0) {
      throw new IllegalArgumentException("practicalScore must not be negative");
    }
    if (rule.standardDurationSeconds() <= 0) {
      throw new IllegalArgumentException("standardDurationSeconds must be positive");
    }
    if (rule.finalScoreUpperLimit() == null || rule.finalScoreUpperLimit().signum() <= 0) {
      throw new IllegalArgumentException("finalScoreUpperLimit must be positive");
    }

    Duration elapsed = Duration.between(startTime, finishTime);
    long elapsedSeconds = elapsed.getSeconds();
    long pausedSeconds = durationSeconds(pausedDuration);
    long effectiveSeconds = Math.max(0, elapsedSeconds - Math.min(elapsedSeconds, pausedSeconds));
    long overtimeSeconds = Math.max(0, effectiveSeconds - rule.standardDurationSeconds());
    long earlySeconds = Math.max(0, rule.standardDurationSeconds() - effectiveSeconds);

    BigDecimal bonus = ZERO;
    BigDecimal deduction = ZERO;
    if (rule.timeScoringEnabled()) {
      deduction = overtimeDeduction(rule, overtimeSeconds);
      bonus = earlyFinishBonus(rule, earlySeconds);
    }

    BigDecimal finalScore = practicalScore.add(bonus).subtract(deduction)
        .max(ZERO).min(rule.finalScoreUpperLimit());
    boolean autoFinish = "AUTO_FINISH".equals(rule.overtimeAction())
        && rule.maxDurationSeconds() != null
        && effectiveSeconds >= rule.maxDurationSeconds();
    String detail = detail(elapsedSeconds, pausedSeconds, effectiveSeconds,
        practicalScore, bonus, deduction, finalScore, rule.finalScoreUpperLimit());
    return new PracticalTimeScoreCalculation(elapsedSeconds, pausedSeconds, effectiveSeconds,
        overtimeSeconds, earlySeconds, bonus, deduction, practicalScore, finalScore,
        autoFinish, detail);
  }

  private BigDecimal overtimeDeduction(PracticalTimeScoreRule rule, long overtimeSeconds) {
    if (overtimeSeconds <= 0 || "NONE".equals(rule.overtimeRuleType())) return ZERO;
    if ("FIXED_DEDUCTION".equals(rule.overtimeRuleType())) {
      return nonNegative(rule.overtimeFixedDeduction(), "overtimeFixedDeduction");
    }
    if (!"STEP_DEDUCTION".equals(rule.overtimeRuleType())) {
      throw new IllegalArgumentException("Unsupported overtimeRuleType");
    }
    long step = positive(rule.overtimeStepSeconds(), "overtimeStepSeconds");
    long steps = overtimeSeconds / step + (overtimeSeconds % step == 0 ? 0 : 1);
    BigDecimal raw = nonNegative(rule.overtimeScorePerStep(), "overtimeScorePerStep")
        .multiply(BigDecimal.valueOf(steps));
    return cap(raw, rule.overtimeMaxDeduction());
  }

  private BigDecimal earlyFinishBonus(PracticalTimeScoreRule rule, long earlySeconds) {
    if (earlySeconds <= 0 || "NONE".equals(rule.earlyFinishRuleType())) return ZERO;
    if ("FIXED_BONUS".equals(rule.earlyFinishRuleType())) {
      return nonNegative(rule.earlyFinishFixedBonus(), "earlyFinishFixedBonus");
    }
    if (!"STEP_BONUS".equals(rule.earlyFinishRuleType())) {
      throw new IllegalArgumentException("Unsupported earlyFinishRuleType");
    }
    long step = positive(rule.earlyFinishStepSeconds(), "earlyFinishStepSeconds");
    long steps = earlySeconds / step;
    BigDecimal raw = nonNegative(rule.earlyFinishScorePerStep(), "earlyFinishScorePerStep")
        .multiply(BigDecimal.valueOf(steps));
    return cap(raw, rule.earlyFinishMaxBonus());
  }

  private BigDecimal cap(BigDecimal score, BigDecimal maximum) {
    return maximum == null ? score : score.min(nonNegative(maximum, "maximum score"));
  }

  private BigDecimal nonNegative(BigDecimal value, String name) {
    if (value == null || value.signum() < 0) {
      throw new IllegalArgumentException(name + " must not be negative or null");
    }
    return value;
  }

  private long positive(Long value, String name) {
    if (value == null || value <= 0) throw new IllegalArgumentException(name + " must be positive");
    return value;
  }

  private long durationSeconds(Duration duration) {
    if (duration == null || duration.isNegative()) {
      throw new IllegalArgumentException("pausedDuration must not be negative");
    }
    return duration.getSeconds();
  }

  private String detail(long elapsed, long paused, long effective,
      BigDecimal practicalScore, BigDecimal bonus, BigDecimal deduction,
      BigDecimal finalScore, BigDecimal upperLimit) {
    return "实际经过 " + format(elapsed) + "，暂停 " + format(paused)
        + "，有效用时 " + format(effective) + "；操作得分 " + score(practicalScore)
        + "；提前完成奖励 +" + score(bonus) + "；超时扣分 -" + score(deduction)
        + "；最终成绩 " + score(finalScore) + "（上限 " + score(upperLimit) + "）";
  }

  private String format(long seconds) {
    return (seconds / 60) + "分" + (seconds % 60) + "秒";
  }

  private String score(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }
}
