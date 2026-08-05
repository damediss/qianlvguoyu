package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_stage_reference_snapshot")
public class CompetitionStageReferenceSnapshotEntity extends BaseEntity {
  private Long settlementId;
  private Long planId;
  private Long eventId;
  private Long stageId;
  private Long targetStageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String promotionRuleType;
  private Integer promotionQuota;
  private Integer promotionPercentage;
  private Integer baseCount;
  private Integer calculatedTargetCount;
  private String status;
  private Long confirmedBy;
  private LocalDateTime confirmedAt;

  public Long getSettlementId() { return settlementId; }
  public void setSettlementId(Long value) { settlementId = value; }
  public Long getPlanId() { return planId; }
  public void setPlanId(Long value) { planId = value; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getTargetStageId() { return targetStageId; }
  public void setTargetStageId(Long value) { targetStageId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { eventWorkTypeLevelId = value; }
  public String getPromotionRuleType() { return promotionRuleType; }
  public void setPromotionRuleType(String value) { promotionRuleType = value; }
  public Integer getPromotionQuota() { return promotionQuota; }
  public void setPromotionQuota(Integer value) { promotionQuota = value; }
  public Integer getPromotionPercentage() { return promotionPercentage; }
  public void setPromotionPercentage(Integer value) { promotionPercentage = value; }
  public Integer getBaseCount() { return baseCount; }
  public void setBaseCount(Integer value) { baseCount = value; }
  public Integer getCalculatedTargetCount() { return calculatedTargetCount; }
  public void setCalculatedTargetCount(Integer value) { calculatedTargetCount = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public Long getConfirmedBy() { return confirmedBy; }
  public void setConfirmedBy(Long value) { confirmedBy = value; }
  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime value) { confirmedAt = value; }
}
