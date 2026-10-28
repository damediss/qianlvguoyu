package com.guoyu.platform.competition.prepractice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_pre_practice_dispatch_recipient")
public class CompetitionPrePracticeDispatchRecipientEntity extends BaseEntity {

  private Long batchId;
  private Long taskId;
  private Long playerId;
  private Long contestantProfileId;
  private Long registrationId;
  private Long orgId;
  private String orgNameSnapshot;
  private String status;
  private LocalDateTime deliveredAt;

  public Long getBatchId() { return batchId; }
  public void setBatchId(Long batchId) { this.batchId = batchId; }
  public Long getTaskId() { return taskId; }
  public void setTaskId(Long taskId) { this.taskId = taskId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public Long getContestantProfileId() { return contestantProfileId; }
  public void setContestantProfileId(Long contestantProfileId) {
    this.contestantProfileId = contestantProfileId;
  }
  public Long getRegistrationId() { return registrationId; }
  public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgNameSnapshot() { return orgNameSnapshot; }
  public void setOrgNameSnapshot(String orgNameSnapshot) { this.orgNameSnapshot = orgNameSnapshot; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getDeliveredAt() { return deliveredAt; }
  public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
}
