package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_stage_roster_adjustment")
public class CompetitionStageRosterAdjustmentEntity extends BaseEntity {
  private Long snapshotId;
  private Long contestantMemberId;
  private String beforeStatus;
  private String afterStatus;
  private String reason;
  private Long operatedBy;
  private LocalDateTime operatedAt;

  public Long getSnapshotId() { return snapshotId; }
  public void setSnapshotId(Long value) { snapshotId = value; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long value) { contestantMemberId = value; }
  public String getBeforeStatus() { return beforeStatus; }
  public void setBeforeStatus(String value) { beforeStatus = value; }
  public String getAfterStatus() { return afterStatus; }
  public void setAfterStatus(String value) { afterStatus = value; }
  public String getReason() { return reason; }
  public void setReason(String value) { reason = value; }
  public Long getOperatedBy() { return operatedBy; }
  public void setOperatedBy(Long value) { operatedBy = value; }
  public LocalDateTime getOperatedAt() { return operatedAt; }
  public void setOperatedAt(LocalDateTime value) { operatedAt = value; }
}
