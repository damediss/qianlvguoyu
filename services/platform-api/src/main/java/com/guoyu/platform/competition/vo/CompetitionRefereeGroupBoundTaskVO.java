package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class CompetitionRefereeGroupBoundTaskVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventId;
  private String eventName;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long stageId;
  private String stageName;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeId;
  private String workTypeName;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeLevelId;
  private String levelName;
  private Integer contestantCount;
  private Integer completedCount;
  private String status;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public String getEventName() { return eventName; }
  public void setEventName(String eventName) { this.eventName = eventName; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public String getStageName() { return stageName; }
  public void setStageName(String stageName) { this.stageName = stageName; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getWorkTypeName() { return workTypeName; }
  public void setWorkTypeName(String workTypeName) { this.workTypeName = workTypeName; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public String getLevelName() { return levelName; }
  public void setLevelName(String levelName) { this.levelName = levelName; }
  public Integer getContestantCount() { return contestantCount; }
  public void setContestantCount(Integer contestantCount) { this.contestantCount = contestantCount; }
  public Integer getCompletedCount() { return completedCount; }
  public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
