package com.guoyu.platform.competition.prepractice.mapper.row;

import java.time.LocalDateTime;

public class CompetitionPlayerPrePracticeInboxRow {
  private Long deliveryId;
  private Long batchId;
  private Long taskId;
  private String taskTitleSnapshot;
  private Long eventId;
  private String eventNameSnapshot;
  private Long eventWorkTypeId;
  private String eventWorkTypeNameSnapshot;
  private String eventWorkTypeLevelCode;
  private String eventWorkTypeLevelNameSnapshot;
  private Long stageId;
  private String stageNameSnapshot;
  private Integer questionCount;
  private String status;
  private LocalDateTime deliveredAt;
  private LocalDateTime pushedAt;

  public Long getDeliveryId() { return deliveryId; }
  public void setDeliveryId(Long deliveryId) { this.deliveryId = deliveryId; }
  public Long getBatchId() { return batchId; }
  public void setBatchId(Long batchId) { this.batchId = batchId; }
  public Long getTaskId() { return taskId; }
  public void setTaskId(Long taskId) { this.taskId = taskId; }
  public String getTaskTitleSnapshot() { return taskTitleSnapshot; }
  public void setTaskTitleSnapshot(String taskTitleSnapshot) { this.taskTitleSnapshot = taskTitleSnapshot; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public String getEventNameSnapshot() { return eventNameSnapshot; }
  public void setEventNameSnapshot(String eventNameSnapshot) { this.eventNameSnapshot = eventNameSnapshot; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getEventWorkTypeNameSnapshot() { return eventWorkTypeNameSnapshot; }
  public void setEventWorkTypeNameSnapshot(String eventWorkTypeNameSnapshot) {
    this.eventWorkTypeNameSnapshot = eventWorkTypeNameSnapshot;
  }
  public String getEventWorkTypeLevelCode() { return eventWorkTypeLevelCode; }
  public void setEventWorkTypeLevelCode(String eventWorkTypeLevelCode) {
    this.eventWorkTypeLevelCode = eventWorkTypeLevelCode;
  }
  public String getEventWorkTypeLevelNameSnapshot() { return eventWorkTypeLevelNameSnapshot; }
  public void setEventWorkTypeLevelNameSnapshot(String eventWorkTypeLevelNameSnapshot) {
    this.eventWorkTypeLevelNameSnapshot = eventWorkTypeLevelNameSnapshot;
  }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public String getStageNameSnapshot() { return stageNameSnapshot; }
  public void setStageNameSnapshot(String stageNameSnapshot) { this.stageNameSnapshot = stageNameSnapshot; }
  public Integer getQuestionCount() { return questionCount; }
  public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getDeliveredAt() { return deliveredAt; }
  public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
  public LocalDateTime getPushedAt() { return pushedAt; }
  public void setPushedAt(LocalDateTime pushedAt) { this.pushedAt = pushedAt; }
}
