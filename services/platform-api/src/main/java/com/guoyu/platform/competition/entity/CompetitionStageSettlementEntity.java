package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_stage_settlement")
public class CompetitionStageSettlementEntity extends BaseEntity {
  private Long planId; private Long eventId; private Long stageId; private Long eventWorkTypeId; private Long eventWorkTypeLevelId;
  private Long scoreRuleId; private Integer settlementVersion; private String status; private String readinessStatus;
  private Long rankingSnapshotId; private Long promotionSnapshotId; private LocalDateTime publishedAt; private Long publishedBy;
  public Long getPlanId() { return planId; } public void setPlanId(Long v) { planId = v; }
  public Long getEventId() { return eventId; } public void setEventId(Long v) { eventId = v; }
  public Long getStageId() { return stageId; } public void setStageId(Long v) { stageId = v; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; } public void setEventWorkTypeId(Long v) { eventWorkTypeId = v; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; } public void setEventWorkTypeLevelId(Long v) { eventWorkTypeLevelId = v; }
  public Long getScoreRuleId() { return scoreRuleId; } public void setScoreRuleId(Long v) { scoreRuleId = v; }
  public Integer getSettlementVersion() { return settlementVersion; } public void setSettlementVersion(Integer v) { settlementVersion = v; }
  public String getStatus() { return status; } public void setStatus(String v) { status = v; }
  public String getReadinessStatus() { return readinessStatus; } public void setReadinessStatus(String v) { readinessStatus = v; }
  public Long getRankingSnapshotId() { return rankingSnapshotId; } public void setRankingSnapshotId(Long v) { rankingSnapshotId = v; }
  public Long getPromotionSnapshotId() { return promotionSnapshotId; } public void setPromotionSnapshotId(Long v) { promotionSnapshotId = v; }
  public LocalDateTime getPublishedAt() { return publishedAt; } public void setPublishedAt(LocalDateTime v) { publishedAt = v; }
  public Long getPublishedBy() { return publishedBy; } public void setPublishedBy(Long v) { publishedBy = v; }
}
