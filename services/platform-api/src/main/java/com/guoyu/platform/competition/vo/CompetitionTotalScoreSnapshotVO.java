package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompetitionTotalScoreSnapshotVO(
    @JsonSerialize(using = ToStringSerializer.class) Long id,
    @JsonSerialize(using = ToStringSerializer.class) Long eventId,
    @JsonSerialize(using = ToStringSerializer.class) Long stageId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeId,
    @JsonSerialize(using = ToStringSerializer.class) Long eventWorkTypeLevelId,
    @JsonSerialize(using = ToStringSerializer.class) Long contestantMemberId,
    @JsonSerialize(using = ToStringSerializer.class) Long scoreRuleId,
    @JsonSerialize(using = ToStringSerializer.class) Long theorySourceId,
    @JsonSerialize(using = ToStringSerializer.class) Long simulationSourceId,
    @JsonSerialize(using = ToStringSerializer.class) Long practicalSourceId,
    BigDecimal theoryScore,
    BigDecimal simulationScore,
    BigDecimal practicalScore,
    BigDecimal simulationPracticalScore,
    BigDecimal theoryWeight,
    BigDecimal simulationWeight,
    BigDecimal practicalWeight,
    BigDecimal simulationPracticalWeight,
    BigDecimal totalScore,
    Integer calculationVersion,
    String confirmationStatus,
    Integer currentFlag,
    LocalDateTime calculatedAt,
    LocalDateTime createdAt) {

  public CompetitionTotalScoreSnapshotVO(
      Long id,
      Long eventId,
      Long stageId,
      Long eventWorkTypeId,
      Long contestantMemberId,
      Long scoreRuleId,
      Long theorySourceId,
      Long simulationSourceId,
      Long practicalSourceId,
      BigDecimal theoryScore,
      BigDecimal simulationScore,
      BigDecimal practicalScore,
      BigDecimal simulationPracticalScore,
      BigDecimal theoryWeight,
      BigDecimal simulationPracticalWeight,
      BigDecimal totalScore,
      Integer calculationVersion,
      String confirmationStatus,
      Integer currentFlag,
      LocalDateTime calculatedAt,
      LocalDateTime createdAt) {
    this(id, eventId, stageId, eventWorkTypeId, null, contestantMemberId, scoreRuleId,
        theorySourceId, simulationSourceId, practicalSourceId, theoryScore, simulationScore,
        practicalScore, simulationPracticalScore, theoryWeight, null, null,
        simulationPracticalWeight, totalScore, calculationVersion, confirmationStatus,
        currentFlag, calculatedAt, createdAt);
  }
}
