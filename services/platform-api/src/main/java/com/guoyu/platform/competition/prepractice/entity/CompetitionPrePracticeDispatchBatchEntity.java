package com.guoyu.platform.competition.prepractice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_pre_practice_dispatch_batch")
public class CompetitionPrePracticeDispatchBatchEntity extends BaseEntity {

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
  private Integer recipientCount;
  private LocalDateTime pushedAt;

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
  public Integer getRecipientCount() { return recipientCount; }
  public void setRecipientCount(Integer recipientCount) { this.recipientCount = recipientCount; }
  public LocalDateTime getPushedAt() { return pushedAt; }
  public void setPushedAt(LocalDateTime pushedAt) { this.pushedAt = pushedAt; }
}
