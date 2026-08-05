package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("competition_theory_attempt")
public class CompetitionTheoryAttemptEntity extends BaseEntity {

  private Long assignmentId;
  private Long scheduleVersionId;
  private Long sessionId;
  private Long paperSnapshotId;
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private Long contestantProfileId;
  private Long contestantMemberId;
  private Long playerId;
  private String status;
  private String sessionState;
  private LocalDateTime lastSessionEventAt;
  private LocalDateTime startedAt;
  private LocalDateTime submittedAt;
  private String submitType;
  private String submitRequestId;
  private BigDecimal totalScore;
  private Long scoreSourceId;
  private Integer absenceFlag;
  private LocalDateTime provisionalAbsentAt;
  private LocalDateTime absenceFinalizedAt;
  private Integer restoreCount;
  private LocalDateTime lastRestoredAt;
  private LocalDateTime lastSavedAt;
  private Long forcedBy;
  private String forceReason;
  private Integer lockVersion;

  public Long getAssignmentId() { return assignmentId; }
  public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
  public Long getScheduleVersionId() { return scheduleVersionId; }
  public void setScheduleVersionId(Long scheduleVersionId) { this.scheduleVersionId = scheduleVersionId; }
  public Long getSessionId() { return sessionId; }
  public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
  public Long getPaperSnapshotId() { return paperSnapshotId; }
  public void setPaperSnapshotId(Long paperSnapshotId) { this.paperSnapshotId = paperSnapshotId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public Long getContestantProfileId() { return contestantProfileId; }
  public void setContestantProfileId(Long contestantProfileId) { this.contestantProfileId = contestantProfileId; }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) { this.contestantMemberId = contestantMemberId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getSessionState() { return sessionState; }
  public void setSessionState(String sessionState) { this.sessionState = sessionState; }
  public LocalDateTime getLastSessionEventAt() { return lastSessionEventAt; }
  public void setLastSessionEventAt(LocalDateTime lastSessionEventAt) { this.lastSessionEventAt = lastSessionEventAt; }
  public LocalDateTime getStartedAt() { return startedAt; }
  public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
  public LocalDateTime getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
  public String getSubmitType() { return submitType; }
  public void setSubmitType(String submitType) { this.submitType = submitType; }
  public String getSubmitRequestId() { return submitRequestId; }
  public void setSubmitRequestId(String submitRequestId) { this.submitRequestId = submitRequestId; }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
  public Long getScoreSourceId() { return scoreSourceId; }
  public void setScoreSourceId(Long scoreSourceId) { this.scoreSourceId = scoreSourceId; }
  public Integer getAbsenceFlag() { return absenceFlag; }
  public void setAbsenceFlag(Integer absenceFlag) { this.absenceFlag = absenceFlag; }
  public LocalDateTime getProvisionalAbsentAt() { return provisionalAbsentAt; }
  public void setProvisionalAbsentAt(LocalDateTime provisionalAbsentAt) { this.provisionalAbsentAt = provisionalAbsentAt; }
  public LocalDateTime getAbsenceFinalizedAt() { return absenceFinalizedAt; }
  public void setAbsenceFinalizedAt(LocalDateTime absenceFinalizedAt) { this.absenceFinalizedAt = absenceFinalizedAt; }
  public Integer getRestoreCount() { return restoreCount; }
  public void setRestoreCount(Integer restoreCount) { this.restoreCount = restoreCount; }
  public LocalDateTime getLastRestoredAt() { return lastRestoredAt; }
  public void setLastRestoredAt(LocalDateTime lastRestoredAt) { this.lastRestoredAt = lastRestoredAt; }
  public LocalDateTime getLastSavedAt() { return lastSavedAt; }
  public void setLastSavedAt(LocalDateTime lastSavedAt) { this.lastSavedAt = lastSavedAt; }
  public Long getForcedBy() { return forcedBy; }
  public void setForcedBy(Long forcedBy) { this.forcedBy = forcedBy; }
  public String getForceReason() { return forceReason; }
  public void setForceReason(String forceReason) { this.forceReason = forceReason; }
  public Integer getLockVersion() { return lockVersion; }
  public void setLockVersion(Integer lockVersion) { this.lockVersion = lockVersion; }
}
