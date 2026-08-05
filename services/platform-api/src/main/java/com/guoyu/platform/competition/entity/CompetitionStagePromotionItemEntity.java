package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_stage_promotion_item")
public class CompetitionStagePromotionItemEntity extends BaseEntity {
  private Long promotionSnapshotId; private Long contestantMemberId; private Integer sourceRank; private String selectionStatus; private String reason;
  public Long getPromotionSnapshotId() { return promotionSnapshotId; } public void setPromotionSnapshotId(Long v) { promotionSnapshotId = v; }
  public Long getContestantMemberId() { return contestantMemberId; } public void setContestantMemberId(Long v) { contestantMemberId = v; }
  public Integer getSourceRank() { return sourceRank; } public void setSourceRank(Integer v) { sourceRank = v; }
  public String getSelectionStatus() { return selectionStatus; } public void setSelectionStatus(String v) { selectionStatus = v; }
  public String getReason() { return reason; } public void setReason(String v) { reason = v; }
}
