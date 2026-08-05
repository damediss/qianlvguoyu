package com.guoyu.platform.competition.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CompetitionWorkTypeStageConfigReq(
    @NotNull Long stageId,
    @NotNull Boolean enabled,
    @PositiveOrZero Integer sortOrder,
    String promotionRuleType,
    Integer promotionPercentage,
    @PositiveOrZero Integer promotionQuota) { }
