package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_stage_lifecycle_history")
public class CompetitionStageLifecycleHistoryEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private String fromStatus;
  private String toStatus;
  private String action;
  private String reason;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public String getFromStatus() { return fromStatus; }
  public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
  public String getToStatus() { return toStatus; }
  public void setToStatus(String toStatus) { this.toStatus = toStatus; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
}
