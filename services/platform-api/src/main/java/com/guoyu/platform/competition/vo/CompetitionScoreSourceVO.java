package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionScoreSourceVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long eventId,
    @JsonSerialize(using = ToStringSerializer.class) Long stageId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeLevelId,
    @JsonSerialize(using = ToStringSerializer.class) Long contestantMemberId,
    String sourceType,
    BigDecimal rawScore,
    String confirmationStatus,
    String sourceSystem,
    String sourceReference,
    @JsonSerialize(using = ToStringSerializer.class) Long scoreRuleId,
    Integer revisionNo,
    Integer currentFlag,
    LocalDateTime achievedAt,
    LocalDateTime confirmedAt,
    @JsonSerialize(using = ToStringSerializer.class) Long confirmedBy,
    LocalDateTime createdAt,
    @JsonSerialize(using = ToStringSerializer.class) Long createdBy) {

  public CompetitionScoreSourceVO(
      Long id,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long eventWorkTypeLevelId,
      Long contestantMemberId,
      String sourceType,
      BigDecimal rawScore,
      String confirmationStatus,
      String sourceSystem,
      String sourceReference,
      Long scoreRuleId,
      Integer revisionNo,
      Integer currentFlag,
      LocalDateTime achievedAt,
      LocalDateTime confirmedAt,
      Long confirmedBy,
      LocalDateTime createdAt) {
    this(id, eventId, stageId, eventWorkTypeId, eventWorkTypeLevelId, contestantMemberId,
        sourceType, rawScore, confirmationStatus, sourceSystem, sourceReference, scoreRuleId,
        revisionNo, currentFlag, achievedAt, confirmedAt, confirmedBy, createdAt, null);
  }

  public CompetitionScoreSourceVO(
      Long id,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long contestantMemberId,
      String sourceType,
      BigDecimal rawScore,
      String confirmationStatus,
      String sourceSystem,
      String sourceReference,
      Long scoreRuleId,
      Integer revisionNo,
      Integer currentFlag,
      LocalDateTime achievedAt,
      LocalDateTime confirmedAt,
      Long confirmedBy,
      LocalDateTime createdAt) {
    this(id, eventId, stageId, eventWorkTypeId, null, contestantMemberId, sourceType, rawScore,
        confirmationStatus, sourceSystem, sourceReference, scoreRuleId, revisionNo, currentFlag,
        achievedAt, confirmedAt, confirmedBy, createdAt, null);
  }
}
