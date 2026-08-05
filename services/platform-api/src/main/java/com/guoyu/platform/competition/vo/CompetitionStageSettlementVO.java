package com.guoyu.platform.competition.vo;
import java.time.LocalDateTime;
import java.util.List;
public record CompetitionStageSettlementVO(Long id, Long planId, Long eventId, Long stageId, Long eventWorkTypeId, Long eventWorkTypeLevelId, Long scoreRuleId, Integer settlementVersion, String status, String readinessStatus, Long rankingSnapshotId, Long promotionSnapshotId, LocalDateTime publishedAt, String promotionRuleType, Integer promotionPercentage, Integer calculatedTargetCount, String referenceStatus, String promotionStatus, String finalPodiumStatus, List<CompetitionStageSettlementItemVO> items) { }
