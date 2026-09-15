package com.guoyu.platform.competition.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
public record CompetitionStageSettlementCreateReq(
    @NotNull Long planId,
    @NotNull Long scoreRuleId,
    @NotNull Long rankingSnapshotId,
    @PositiveOrZero Integer promotionQuota,
    Long targetStageId) { }
