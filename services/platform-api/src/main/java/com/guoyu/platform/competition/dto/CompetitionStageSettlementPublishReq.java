package com.guoyu.platform.competition.dto;
import jakarta.validation.constraints.PositiveOrZero;
public record CompetitionStageSettlementPublishReq(@PositiveOrZero Integer promotionQuota, Long targetStageId) { }
