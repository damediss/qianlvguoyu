package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CompetitionStageExecutionPlanVO(
    Long id,
    Long eventId,
    Long stageId,
    Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    Integer planVersion,
    String status,
    String resourceVersion,
    String preflightStatus,
    Long scoreRuleId,
    Integer promotionQuota,
    String promotionRuleType,
    Integer promotionPercentage,
    Integer legacyRule,
    Long targetStageId,
    Long rosterSnapshotId,
    LocalDateTime publishedAt,
    LocalDateTime withdrawnAt,
    String withdrawReason,
    List<CompetitionStagePlanResourceVO> resources) {
}
