package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record CompetitionWorkTypeStageConfigVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long eventId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeId,
    String eventWorkTypeLevelCode,
    @JsonSerialize(using = ToStringSerializer.class) Long stageId,
    String stageType,
    String stageName,
    Integer enabled,
    Integer sortOrder,
    String promotionRuleType,
    Integer promotionPercentage,
    Integer promotionQuota,
    Integer legacyRule) { }
