package com.guoyu.platform.competition.prepractice.vo;

import java.time.LocalDateTime;

public record CompetitionPlayerPrePracticeTaskVO(
    String deliveryId,
    String batchId,
    String taskId,
    String title,
    String eventId,
    String eventName,
    String eventWorkTypeId,
    String eventWorkTypeName,
    String eventWorkTypeLevelCode,
    String eventWorkTypeLevelName,
    String stageId,
    String stageName,
    Integer questionCount,
    String status,
    LocalDateTime deliveredAt,
    LocalDateTime pushedAt,
    boolean effectiveTimeEnabled,
    LocalDateTime effectiveStartAt,
    LocalDateTime effectiveEndAt) {

  public CompetitionPlayerPrePracticeTaskVO(
      String deliveryId,
      String batchId,
      String taskId,
      String title,
      String eventId,
      String eventName,
      String eventWorkTypeId,
      String eventWorkTypeName,
      String eventWorkTypeLevelCode,
      String eventWorkTypeLevelName,
      String stageId,
      String stageName,
      Integer questionCount,
      String status,
      LocalDateTime deliveredAt,
      LocalDateTime pushedAt) {
    this(deliveryId, batchId, taskId, title, eventId, eventName, eventWorkTypeId,
        eventWorkTypeName, eventWorkTypeLevelCode, eventWorkTypeLevelName, stageId, stageName,
        questionCount, status, deliveredAt, pushedAt, false, null, null);
  }
}
