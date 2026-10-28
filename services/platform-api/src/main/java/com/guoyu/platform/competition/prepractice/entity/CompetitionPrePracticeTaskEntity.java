package com.guoyu.platform.competition.prepractice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_pre_practice_task")
public class CompetitionPrePracticeTaskEntity extends BaseEntity {

  private Long eventId;
  private String eventNameSnapshot;
  private Long eventWorkTypeId;
  private String eventWorkTypeNameSnapshot;
  private String eventWorkTypeLevelCode;
  private String eventWorkTypeLevelNameSnapshot;
  private Long stageId;
  private String stageNameSnapshot;
  private String title;
  private String sourceScopeJson;
  private String questionTypeCountsJson;
  private Integer questionCount;
  private String status;
  private String deliveryStatus;

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
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getSourceScopeJson() { return sourceScopeJson; }
  public void setSourceScopeJson(String sourceScopeJson) { this.sourceScopeJson = sourceScopeJson; }
  public String getQuestionTypeCountsJson() { return questionTypeCountsJson; }
  public void setQuestionTypeCountsJson(String questionTypeCountsJson) {
    this.questionTypeCountsJson = questionTypeCountsJson;
  }
  public Integer getQuestionCount() { return questionCount; }
  public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getDeliveryStatus() { return deliveryStatus; }
  public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
}
