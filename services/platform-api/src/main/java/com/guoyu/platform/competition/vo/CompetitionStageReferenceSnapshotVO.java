package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CompetitionStageReferenceSnapshotVO(
    Long id,
    Long settlementId,
    Long planId,
    Long eventId,
    Long stageId,
    Long targetStageId,
    Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    String promotionRuleType,
    Integer promotionQuota,
    Integer promotionPercentage,
    Integer baseCount,
    Integer calculatedTargetCount,
    String status,
    Long confirmedBy,
    LocalDateTime confirmedAt,
    List<CompetitionStageReferenceItemVO> items) { }
