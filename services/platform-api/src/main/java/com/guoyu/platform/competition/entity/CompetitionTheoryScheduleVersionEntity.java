package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_theory_schedule_version")
public class CompetitionTheoryScheduleVersionEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long scoreRuleId;
  private Integer versionNo;
  private String status;
  private String randomBatchId;
  private String randomSeed;
  private LocalDateTime generatedAt;
  private Long generatedBy;
  private LocalDateTime publishedAt;
  private Long publishedBy;
  private LocalDateTime withdrawnAt;
  private Long withdrawnBy;
  private String withdrawReason;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public Long getScoreRuleId() { return scoreRuleId; }
  public void setScoreRuleId(Long scoreRuleId) { this.scoreRuleId = scoreRuleId; }
  public Integer getVersionNo() { return versionNo; }
  public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getRandomBatchId() { return randomBatchId; }
  public void setRandomBatchId(String randomBatchId) { this.randomBatchId = randomBatchId; }
  public String getRandomSeed() { return randomSeed; }
  public void setRandomSeed(String randomSeed) { this.randomSeed = randomSeed; }
  public LocalDateTime getGeneratedAt() { return generatedAt; }
  public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
  public Long getGeneratedBy() { return generatedBy; }
  public void setGeneratedBy(Long generatedBy) { this.generatedBy = generatedBy; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
  public Long getPublishedBy() { return publishedBy; }
  public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }
  public LocalDateTime getWithdrawnAt() { return withdrawnAt; }
  public void setWithdrawnAt(LocalDateTime withdrawnAt) { this.withdrawnAt = withdrawnAt; }
  public Long getWithdrawnBy() { return withdrawnBy; }
  public void setWithdrawnBy(Long withdrawnBy) { this.withdrawnBy = withdrawnBy; }
  public String getWithdrawReason() { return withdrawReason; }
  public void setWithdrawReason(String withdrawReason) { this.withdrawReason = withdrawReason; }
}
