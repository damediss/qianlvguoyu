package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionPracticalExecutionConfigVO(
    String id,
    String eventId,
    String stageId,
    String stageContentId,
    String eventWorkTypeId,
    String eventWorkTypeLevelId,
    String scoringSchemeId,
    String examName,
    Long durationSeconds,
    String timeUnit,
    String startMode,
    boolean allowEarlySubmit,
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
    BigDecimal finalScoreUpperLimit,
    BigDecimal standardTotalScore,
    Integer version,
    CompetitionPracticalScoringSchemeVO scoringScheme) { }
