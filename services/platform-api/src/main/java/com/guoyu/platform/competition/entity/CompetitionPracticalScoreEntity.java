package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_practical_score")
public class CompetitionPracticalScoreEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long contestantMemberId;
  private Long refereeMemberId;
  private Long refereeAssignmentId;
  private String criteriaVersionReference;
  private Integer revisionNo;
  private String status;
  private BigDecimal totalScore;
  private Integer currentFlag;
  private String sourceSystem;
  private String sourceReference;
  private Long replacedScoreId;
  private String replacementReason;
  private LocalDateTime submittedAt;
  private LocalDateTime lockedAt;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) {
    this.contestantMemberId = contestantMemberId;
  }
  public Long getRefereeMemberId() { return refereeMemberId; }
  public void setRefereeMemberId(Long refereeMemberId) { this.refereeMemberId = refereeMemberId; }
  public Long getRefereeAssignmentId() { return refereeAssignmentId; }
  public void setRefereeAssignmentId(Long refereeAssignmentId) {
    this.refereeAssignmentId = refereeAssignmentId;
  }
  public String getCriteriaVersionReference() { return criteriaVersionReference; }
  public void setCriteriaVersionReference(String criteriaVersionReference) {
    this.criteriaVersionReference = criteriaVersionReference;
  }
  public Integer getRevisionNo() { return revisionNo; }
  public void setRevisionNo(Integer revisionNo) { this.revisionNo = revisionNo; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
  public Integer getCurrentFlag() { return currentFlag; }
  public void setCurrentFlag(Integer currentFlag) { this.currentFlag = currentFlag; }
  public String getSourceSystem() { return sourceSystem; }
  public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
  public String getSourceReference() { return sourceReference; }
  public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
  public Long getReplacedScoreId() { return replacedScoreId; }
  public void setReplacedScoreId(Long replacedScoreId) { this.replacedScoreId = replacedScoreId; }
  public String getReplacementReason() { return replacementReason; }
  public void setReplacementReason(String replacementReason) {
    this.replacementReason = replacementReason;
  }
  public LocalDateTime getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
  public LocalDateTime getLockedAt() { return lockedAt; }
  public void setLockedAt(LocalDateTime lockedAt) { this.lockedAt = lockedAt; }
}
