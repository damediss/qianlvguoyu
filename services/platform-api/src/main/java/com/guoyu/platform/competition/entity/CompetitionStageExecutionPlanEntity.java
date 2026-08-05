package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_stage_execution_plan")
public class CompetitionStageExecutionPlanEntity extends BaseEntity {
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Integer planVersion;
  private String status;
  private String resourceVersion;
  private String preflightStatus;
  private Long scoreRuleId;
  private Integer promotionQuota;
  private String promotionRuleType;
  private Integer promotionPercentage;
  private Integer legacyRule;
  private Long targetStageId;
  private Long rosterSnapshotId;
  private LocalDateTime publishedAt;
  private Long publishedBy;
  private LocalDateTime withdrawnAt;
  private Long withdrawnBy;
  private String withdrawReason;

  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { eventWorkTypeLevelId = value; }
  public Integer getPlanVersion() { return planVersion; }
  public void setPlanVersion(Integer value) { planVersion = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public String getResourceVersion() { return resourceVersion; }
  public void setResourceVersion(String value) { resourceVersion = value; }
  public String getPreflightStatus() { return preflightStatus; }
  public void setPreflightStatus(String value) { preflightStatus = value; }
  public Long getScoreRuleId() { return scoreRuleId; }
  public void setScoreRuleId(Long value) { scoreRuleId = value; }
  public Integer getPromotionQuota() { return promotionQuota; }
  public void setPromotionQuota(Integer value) { promotionQuota = value; }
  public String getPromotionRuleType() { return promotionRuleType; }
  public void setPromotionRuleType(String value) { promotionRuleType = value; }
  public Integer getPromotionPercentage() { return promotionPercentage; }
  public void setPromotionPercentage(Integer value) { promotionPercentage = value; }
  public Integer getLegacyRule() { return legacyRule; }
  public void setLegacyRule(Integer value) { legacyRule = value; }
  public Long getTargetStageId() { return targetStageId; }
  public void setTargetStageId(Long value) { targetStageId = value; }
  public Long getRosterSnapshotId() { return rosterSnapshotId; }
  public void setRosterSnapshotId(Long value) { rosterSnapshotId = value; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime value) { publishedAt = value; }
  public Long getPublishedBy() { return publishedBy; }
  public void setPublishedBy(Long value) { publishedBy = value; }
  public LocalDateTime getWithdrawnAt() { return withdrawnAt; }
  public void setWithdrawnAt(LocalDateTime value) { withdrawnAt = value; }
  public Long getWithdrawnBy() { return withdrawnBy; }
  public void setWithdrawnBy(Long value) { withdrawnBy = value; }
  public String getWithdrawReason() { return withdrawReason; }
  public void setWithdrawReason(String value) { withdrawReason = value; }
}
