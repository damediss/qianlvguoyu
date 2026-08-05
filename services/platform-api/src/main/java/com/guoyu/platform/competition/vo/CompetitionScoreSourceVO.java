package com.guoyu.platform.competition.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionScoreSourceVO(
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
        achievedAt, confirmedAt, confirmedBy, createdAt);
  }
}
