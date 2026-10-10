package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_ranking_snapshot")
public class CompetitionRankingSnapshotEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long scoreRuleId;
  private Integer snapshotVersion;
  private String inputFingerprint;
  private String status;
  private String generationReason;
  private LocalDateTime confirmedAt;
  private Long confirmedBy;
  private LocalDateTime publishedAt;
  private Long publishedBy;
  private LocalDateTime supersededAt;
  private Long supersededBy;
  private Long supersedesSnapshotId;
  private String correctionReason;
  private LocalDateTime generatedAt;

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
  public Integer getSnapshotVersion() { return snapshotVersion; }
  public void setSnapshotVersion(Integer snapshotVersion) { this.snapshotVersion = snapshotVersion; }
  public String getInputFingerprint() { return inputFingerprint; }
  public void setInputFingerprint(String inputFingerprint) { this.inputFingerprint = inputFingerprint; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getGenerationReason() { return generationReason; }
  public void setGenerationReason(String generationReason) {
    this.generationReason = generationReason;
  }
  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
  public Long getConfirmedBy() { return confirmedBy; }
  public void setConfirmedBy(Long confirmedBy) { this.confirmedBy = confirmedBy; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
  public Long getPublishedBy() { return publishedBy; }
  public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }
  public LocalDateTime getSupersededAt() { return supersededAt; }
  public void setSupersededAt(LocalDateTime supersededAt) { this.supersededAt = supersededAt; }
  public Long getSupersededBy() { return supersededBy; }
  public void setSupersededBy(Long supersededBy) { this.supersededBy = supersededBy; }
  public Long getSupersedesSnapshotId() { return supersedesSnapshotId; }
  public void setSupersedesSnapshotId(Long supersedesSnapshotId) { this.supersedesSnapshotId = supersedesSnapshotId; }
  public String getCorrectionReason() { return correctionReason; }
  public void setCorrectionReason(String correctionReason) { this.correctionReason = correctionReason; }
  public LocalDateTime getGeneratedAt() { return generatedAt; }
  public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
