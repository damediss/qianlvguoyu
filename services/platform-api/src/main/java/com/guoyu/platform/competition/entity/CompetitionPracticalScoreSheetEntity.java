package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_practical_score_sheet")
public class CompetitionPracticalScoreSheetEntity extends BaseEntity {
  private Long assignmentId;
  private Long planId;
  private Long ruleId;
  private Long assessmentSheetId;
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long contestantMemberId;
  private Long refereeMemberId;
  private Integer revisionNo;
  private String status;
  private BigDecimal totalScore;
  private String unlockReason;
  private LocalDateTime openedAt;
  private LocalDateTime submittedAt;
  private LocalDateTime lockedAt;
  public Long getAssignmentId() { return assignmentId; }
  public void setAssignmentId(Long value) { assignmentId = value; }
  public Long getPlanId() { return planId; }
  public void setPlanId(Long value) { planId = value; }
  public Long getRuleId() { return ruleId; }
  public void setRuleId(Long value) { ruleId = value; }
  public Long getAssessmentSheetId() { return assessmentSheetId; }
  public void setAssessmentSheetId(Long value) { assessmentSheetId = value; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { eventWorkTypeLevelId = value; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long value) { contestantMemberId = value; }
  public Long getRefereeMemberId() { return refereeMemberId; }
  public void setRefereeMemberId(Long value) { refereeMemberId = value; }
  public Integer getRevisionNo() { return revisionNo; }
  public void setRevisionNo(Integer value) { revisionNo = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal value) { totalScore = value; }
  public String getUnlockReason() { return unlockReason; }
  public void setUnlockReason(String value) { unlockReason = value; }
  public LocalDateTime getOpenedAt() { return openedAt; }
  public void setOpenedAt(LocalDateTime value) { openedAt = value; }
  public LocalDateTime getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(LocalDateTime value) { submittedAt = value; }
  public LocalDateTime getLockedAt() { return lockedAt; }
  public void setLockedAt(LocalDateTime value) { lockedAt = value; }
}
