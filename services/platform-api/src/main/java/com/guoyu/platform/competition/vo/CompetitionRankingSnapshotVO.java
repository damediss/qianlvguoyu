package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionRankingSnapshotVO(
    Long id,
    Long eventId,
    Long stageId,
    Long eventWorkTypeId,
    Long eventWorkTypeLevelId,
    Long scoreRuleId,
    Integer snapshotVersion,
    String status,
    String generationReason,
    LocalDateTime generatedAt,
    LocalDateTime createdAt) {

  public CompetitionRankingSnapshotVO(
      Long id,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long scoreRuleId,
      Integer snapshotVersion,
      String status,
      String generationReason,
      LocalDateTime generatedAt,
      LocalDateTime createdAt) {
    this(id, eventId, stageId, eventWorkTypeId, null, scoreRuleId, snapshotVersion, status,
        generationReason, generatedAt, createdAt);
  }
}
