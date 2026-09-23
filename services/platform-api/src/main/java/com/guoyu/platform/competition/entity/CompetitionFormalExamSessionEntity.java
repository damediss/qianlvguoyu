package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_formal_exam_session")
public class CompetitionFormalExamSessionEntity extends BaseEntity {

  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String examType;
  private String status;
  private Integer durationSeconds;
  private LocalDateTime plannedStartTime;
  private LocalDateTime actualStartTime;
  private LocalDateTime actualEndTime;
  private Integer candidateTotal;
  private Integer submittedTotal;
  private LocalDateTime deadlineAt;
  private LocalDateTime expireClaimUntil;
  private String expireClaimOwner;
  private Long occupancyToken;
  private String scoringStatus;
  private LocalDateTime scoringStartedAt;
  private LocalDateTime scoredTime;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private String scoringError;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long stageId) { this.stageId = stageId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) {
    this.eventWorkTypeLevelId = eventWorkTypeLevelId;
  }
  public String getExamType() { return examType; }
  public void setExamType(String examType) { this.examType = examType; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getDurationSeconds() { return durationSeconds; }
  public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
  public LocalDateTime getPlannedStartTime() { return plannedStartTime; }
  public void setPlannedStartTime(LocalDateTime plannedStartTime) {
    this.plannedStartTime = plannedStartTime;
  }
  public LocalDateTime getActualStartTime() { return actualStartTime; }
  public void setActualStartTime(LocalDateTime actualStartTime) {
    this.actualStartTime = actualStartTime;
  }
  public LocalDateTime getActualEndTime() { return actualEndTime; }
  public void setActualEndTime(LocalDateTime actualEndTime) { this.actualEndTime = actualEndTime; }
  public Integer getCandidateTotal() { return candidateTotal; }
  public void setCandidateTotal(Integer candidateTotal) { this.candidateTotal = candidateTotal; }
  public Integer getSubmittedTotal() { return submittedTotal; }
  public void setSubmittedTotal(Integer submittedTotal) { this.submittedTotal = submittedTotal; }
  public LocalDateTime getDeadlineAt() { return deadlineAt; }
  public void setDeadlineAt(LocalDateTime deadlineAt) { this.deadlineAt = deadlineAt; }
  public LocalDateTime getExpireClaimUntil() { return expireClaimUntil; }
  public void setExpireClaimUntil(LocalDateTime expireClaimUntil) {
    this.expireClaimUntil = expireClaimUntil;
  }
  public String getExpireClaimOwner() { return expireClaimOwner; }
  public void setExpireClaimOwner(String expireClaimOwner) {
    this.expireClaimOwner = expireClaimOwner;
  }
  public Long getOccupancyToken() { return occupancyToken; }
  public void setOccupancyToken(Long occupancyToken) { this.occupancyToken = occupancyToken; }
  public String getScoringStatus() { return scoringStatus; }
  public void setScoringStatus(String scoringStatus) { this.scoringStatus = scoringStatus; }
  public LocalDateTime getScoringStartedAt() { return scoringStartedAt; }
  public void setScoringStartedAt(LocalDateTime scoringStartedAt) {
    this.scoringStartedAt = scoringStartedAt;
  }
  public LocalDateTime getScoredTime() { return scoredTime; }
  public void setScoredTime(LocalDateTime scoredTime) { this.scoredTime = scoredTime; }
  public String getScoringError() { return scoringError; }
  public void setScoringError(String scoringError) { this.scoringError = scoringError; }
}
