package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public record CompetitionRankingSnapshotVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long eventId,
    @JsonSerialize(using = ToStringSerializer.class) Long stageId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeLevelId,
    @JsonSerialize(using = ToStringSerializer.class) Long scoreRuleId,
    Integer snapshotVersion,
    String status,
    String generationReason,
    LocalDateTime generatedAt,
    LocalDateTime createdAt,
    String inputFingerprint,
    LocalDateTime confirmedAt,
    String confirmedBy,
    LocalDateTime publishedAt,
    String publishedBy,
    LocalDateTime supersededAt,
    String supersededBy,
    String supersedesSnapshotId,
    String correctionReason) {

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
        generationReason, generatedAt, createdAt, null, null, null, null, null,
        null, null, null, null);
  }
}
