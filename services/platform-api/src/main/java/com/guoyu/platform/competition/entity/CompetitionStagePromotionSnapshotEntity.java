package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_stage_promotion_snapshot")
public class CompetitionStagePromotionSnapshotEntity extends BaseEntity {
  private Long settlementId; private Long sourcePlanId; private Long sourceStageId; private Long targetStageId; private Long eventId; private Long eventWorkTypeId; private Long eventWorkTypeLevelId;
  private Integer quota; private String promotionRuleType; private Integer promotionPercentage; private Integer calculatedTargetCount;
  private String referenceStatus; private String promotionStatus; private String finalPodiumStatus;
  private Integer promotionVersion; private String status; private LocalDateTime publishedAt; private Long publishedBy;
  public Long getSettlementId() { return settlementId; } public void setSettlementId(Long v) { settlementId = v; }
  public Long getSourcePlanId() { return sourcePlanId; } public void setSourcePlanId(Long v) { sourcePlanId = v; }
  public Long getSourceStageId() { return sourceStageId; } public void setSourceStageId(Long v) { sourceStageId = v; }
  public Long getTargetStageId() { return targetStageId; } public void setTargetStageId(Long v) { targetStageId = v; }
  public Long getEventId() { return eventId; } public void setEventId(Long v) { eventId = v; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; } public void setEventWorkTypeId(Long v) { eventWorkTypeId = v; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; } public void setEventWorkTypeLevelId(Long v) { eventWorkTypeLevelId = v; }
  public Integer getQuota() { return quota; } public void setQuota(Integer v) { quota = v; }
  public String getPromotionRuleType() { return promotionRuleType; } public void setPromotionRuleType(String v) { promotionRuleType = v; }
  public Integer getPromotionPercentage() { return promotionPercentage; } public void setPromotionPercentage(Integer v) { promotionPercentage = v; }
  public Integer getCalculatedTargetCount() { return calculatedTargetCount; } public void setCalculatedTargetCount(Integer v) { calculatedTargetCount = v; }
  public String getReferenceStatus() { return referenceStatus; } public void setReferenceStatus(String v) { referenceStatus = v; }
  public String getPromotionStatus() { return promotionStatus; } public void setPromotionStatus(String v) { promotionStatus = v; }
  public String getFinalPodiumStatus() { return finalPodiumStatus; } public void setFinalPodiumStatus(String v) { finalPodiumStatus = v; }
  public Integer getPromotionVersion() { return promotionVersion; } public void setPromotionVersion(Integer v) { promotionVersion = v; }
  public String getStatus() { return status; } public void setStatus(String v) { status = v; }
  public LocalDateTime getPublishedAt() { return publishedAt; } public void setPublishedAt(LocalDateTime v) { publishedAt = v; }
  public Long getPublishedBy() { return publishedBy; } public void setPublishedBy(Long v) { publishedBy = v; }
}
