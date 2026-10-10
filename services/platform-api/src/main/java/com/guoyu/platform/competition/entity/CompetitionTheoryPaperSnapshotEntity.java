package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_theory_paper_snapshot")
public class CompetitionTheoryPaperSnapshotEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long scoreRuleId;
  private Long requirementId;
  private String paperCode;
  private Integer versionNo;
  private Long sourceCourseId;
  private Long sourcePlanId;
  private Long sourcePaperId;
  private Long sourceCompetitionPaperSnapshotId;
  private String sourceTitle;
  private String sourceHash;
  private Integer durationMinutes;
  private Integer questionCount;
  private BigDecimal totalScore;
  private BigDecimal passScore;
  private String status;
  private LocalDateTime importedAt;
  private Long importedBy;
  private LocalDateTime publishedAt;
  private Long publishedBy;

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
  public Long getRequirementId() { return requirementId; }
  public void setRequirementId(Long requirementId) { this.requirementId = requirementId; }
  public String getPaperCode() { return paperCode; }
  public void setPaperCode(String paperCode) { this.paperCode = paperCode; }
  public Integer getVersionNo() { return versionNo; }
  public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
  public Long getSourceCourseId() { return sourceCourseId; }
  public void setSourceCourseId(Long sourceCourseId) { this.sourceCourseId = sourceCourseId; }
  public Long getSourcePlanId() { return sourcePlanId; }
  public void setSourcePlanId(Long sourcePlanId) { this.sourcePlanId = sourcePlanId; }
  public Long getSourcePaperId() { return sourcePaperId; }
  public void setSourcePaperId(Long sourcePaperId) { this.sourcePaperId = sourcePaperId; }
  public Long getSourceCompetitionPaperSnapshotId() { return sourceCompetitionPaperSnapshotId; }
  public void setSourceCompetitionPaperSnapshotId(Long sourceCompetitionPaperSnapshotId) {
    this.sourceCompetitionPaperSnapshotId = sourceCompetitionPaperSnapshotId;
  }
  public String getSourceTitle() { return sourceTitle; }
  public void setSourceTitle(String sourceTitle) { this.sourceTitle = sourceTitle; }
  public String getSourceHash() { return sourceHash; }
  public void setSourceHash(String sourceHash) { this.sourceHash = sourceHash; }
  public Integer getDurationMinutes() { return durationMinutes; }
  public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
  public Integer getQuestionCount() { return questionCount; }
  public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
  public BigDecimal getPassScore() { return passScore; }
  public void setPassScore(BigDecimal passScore) { this.passScore = passScore; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getImportedAt() { return importedAt; }
  public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }
  public Long getImportedBy() { return importedBy; }
  public void setImportedBy(Long importedBy) { this.importedBy = importedBy; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
  public Long getPublishedBy() { return publishedBy; }
  public void setPublishedBy(Long publishedBy) { this.publishedBy = publishedBy; }
}
