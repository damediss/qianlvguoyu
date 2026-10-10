package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;

public record CompetitionPracticalScoringSchemeVO(
    String id,
    String sourceEventId,
    String sourceEventWorkTypeId,
    String sourceEventWorkTypeLevelId,
    String title,
    String paperCode,
    Integer versionNo,
    Integer durationMinutes,
    Integer questionCount,
    BigDecimal totalScore,
    String sourceHash) { }
