package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_stage_settlement_item")
public class CompetitionStageSettlementItemEntity extends BaseEntity {
  private Long settlementId; private Long contestantMemberId; private Long totalScoreSnapshotId; private BigDecimal totalScore;
  private String contestantStatus; private Integer displayRank; private Integer tiedFlag; private String promotionStatus; private String reason;
  public Long getSettlementId() { return settlementId; } public void setSettlementId(Long v) { settlementId = v; }
  public Long getContestantMemberId() { return contestantMemberId; } public void setContestantMemberId(Long v) { contestantMemberId = v; }
  public Long getTotalScoreSnapshotId() { return totalScoreSnapshotId; } public void setTotalScoreSnapshotId(Long v) { totalScoreSnapshotId = v; }
  public BigDecimal getTotalScore() { return totalScore; } public void setTotalScore(BigDecimal v) { totalScore = v; }
  public String getContestantStatus() { return contestantStatus; } public void setContestantStatus(String v) { contestantStatus = v; }
  public Integer getDisplayRank() { return displayRank; } public void setDisplayRank(Integer v) { displayRank = v; }
  public Integer getTiedFlag() { return tiedFlag; } public void setTiedFlag(Integer v) { tiedFlag = v; }
  public String getPromotionStatus() { return promotionStatus; } public void setPromotionStatus(String v) { promotionStatus = v; }
  public String getReason() { return reason; } public void setReason(String v) { reason = v; }
}
