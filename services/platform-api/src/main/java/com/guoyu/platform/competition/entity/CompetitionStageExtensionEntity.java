package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_stage_extension")
public class CompetitionStageExtensionEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private LocalDateTime previousStartAt;
  private LocalDateTime previousEndAt;
  private LocalDateTime newStartAt;
  private LocalDateTime newEndAt;
  private String reason;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public LocalDateTime getPreviousStartAt() { return previousStartAt; }
  public void setPreviousStartAt(LocalDateTime previousStartAt) { this.previousStartAt = previousStartAt; }
  public LocalDateTime getPreviousEndAt() { return previousEndAt; }
  public void setPreviousEndAt(LocalDateTime previousEndAt) { this.previousEndAt = previousEndAt; }
  public LocalDateTime getNewStartAt() { return newStartAt; }
  public void setNewStartAt(LocalDateTime newStartAt) { this.newStartAt = newStartAt; }
  public LocalDateTime getNewEndAt() { return newEndAt; }
  public void setNewEndAt(LocalDateTime newEndAt) { this.newEndAt = newEndAt; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
}
