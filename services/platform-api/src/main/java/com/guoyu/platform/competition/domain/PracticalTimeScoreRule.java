package com.guoyu.platform.competition.domain;

import java.math.BigDecimal;

/** Immutable time scoring rules copied from a stage-level practical configuration. */
public record PracticalTimeScoreRule(
    long standardDurationSeconds,
    boolean timeScoringEnabled,
    String overtimeRuleType,
    Long overtimeStepSeconds,
    BigDecimal overtimeScorePerStep,
    BigDecimal overtimeFixedDeduction,
    BigDecimal overtimeMaxDeduction,
    String earlyFinishRuleType,
    Long earlyFinishStepSeconds,
    BigDecimal earlyFinishScorePerStep,
    BigDecimal earlyFinishFixedBonus,
    BigDecimal earlyFinishMaxBonus,
    String overtimeAction,
    Long maxDurationSeconds,
    BigDecimal finalScoreUpperLimit) { }
