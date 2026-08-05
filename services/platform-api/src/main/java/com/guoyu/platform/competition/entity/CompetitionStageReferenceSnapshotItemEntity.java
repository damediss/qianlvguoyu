package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_stage_reference_snapshot_item")
public class CompetitionStageReferenceSnapshotItemEntity extends BaseEntity {
  private Long referenceSnapshotId;
  private Long contestantMemberId;
  private Integer sourceRank;
  private String selectionStatus;
  private String reason;

  public Long getReferenceSnapshotId() { return referenceSnapshotId; }
  public void setReferenceSnapshotId(Long value) { referenceSnapshotId = value; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long value) { contestantMemberId = value; }
  public Integer getSourceRank() { return sourceRank; }
  public void setSourceRank(Integer value) { sourceRank = value; }
  public String getSelectionStatus() { return selectionStatus; }
  public void setSelectionStatus(String value) { selectionStatus = value; }
  public String getReason() { return reason; }
  public void setReason(String value) { reason = value; }
}
