package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CompetitionPracticalExecutionConfigSaveReq(
    @NotNull @Positive Long eventWorkTypeId,
    @NotNull @Positive Long eventWorkTypeLevelId,
    @NotNull @Positive Long scoringSchemeId,
    @NotBlank @Size(max = 128) String examName,
    @NotNull @Positive Long durationSeconds,
    @NotBlank String startMode,
    boolean allowEarlySubmit,
    boolean timeScoringEnabled,
    @NotBlank String overtimeRuleType,
    Long overtimeStepSeconds,
    BigDecimal overtimeScorePerStep,
    BigDecimal overtimeFixedDeduction,
    BigDecimal overtimeMaxDeduction,
    @NotBlank String earlyFinishRuleType,
    Long earlyFinishStepSeconds,
    BigDecimal earlyFinishScorePerStep,
    BigDecimal earlyFinishFixedBonus,
    BigDecimal earlyFinishMaxBonus,
    @NotBlank String overtimeAction,
    Long maxDurationSeconds,
    @NotNull BigDecimal finalScoreUpperLimit) { }
