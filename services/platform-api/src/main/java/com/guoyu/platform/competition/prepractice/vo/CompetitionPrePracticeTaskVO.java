package com.guoyu.platform.competition.prepractice.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record CompetitionPrePracticeTaskVO(
    String id,
    String eventId,
    String eventName,
    String eventWorkTypeId,
    String eventWorkTypeName,
    String eventWorkTypeLevelCode,
    String eventWorkTypeLevelName,
    String stageId,
    String stageName,
    String title,
    JsonNode sourceScope,
    JsonNode questionTypeCounts,
    Integer questionCount,
    String status,
    String deliveryStatus,
    LocalDateTime createdAt,
    boolean effectiveTimeEnabled,
    LocalDateTime effectiveStartAt,
    LocalDateTime effectiveEndAt) {

  public CompetitionPrePracticeTaskVO(
      String id,
      String eventId,
      String eventName,
      String eventWorkTypeId,
      String eventWorkTypeName,
      String eventWorkTypeLevelCode,
      String eventWorkTypeLevelName,
      String stageId,
      String stageName,
      String title,
      JsonNode sourceScope,
      JsonNode questionTypeCounts,
      Integer questionCount,
      String status,
      String deliveryStatus,
      LocalDateTime createdAt) {
    this(id, eventId, eventName, eventWorkTypeId, eventWorkTypeName,
        eventWorkTypeLevelCode, eventWorkTypeLevelName, stageId, stageName, title,
        sourceScope, questionTypeCounts, questionCount, status, deliveryStatus, createdAt,
        false, null, null);
  }
}
