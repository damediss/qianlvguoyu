package com.guoyu.platform.competition.vo;

public record CompetitionWorkTypeStageConfigVO(
    Long id,
    Long eventId,
    Long eventWorkTypeId,
    Long stageId,
    String stageType,
    String stageName,
    Integer enabled,
    Integer sortOrder,
    String promotionRuleType,
    Integer promotionPercentage,
    Integer promotionQuota,
    Integer legacyRule) { }
