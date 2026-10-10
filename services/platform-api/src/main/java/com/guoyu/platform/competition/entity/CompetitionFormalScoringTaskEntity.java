package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_formal_scoring_task")
public class CompetitionFormalScoringTaskEntity extends BaseEntity {
  private Long examSessionId;
  private Long candidateExamId;
  private Integer resultRevisionNo;
  private String taskStatus;
  private Integer attemptCount;
  private LocalDateTime nextAttemptAt;
  private String leaseOwner;
  private LocalDateTime leaseUntil;
  private String lastError;
  private String requestedReason;
  private Long requestedBy;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;

  public Long getExamSessionId() { return examSessionId; }
  public void setExamSessionId(Long value) { this.examSessionId = value; }
  public Long getCandidateExamId() { return candidateExamId; }
  public void setCandidateExamId(Long value) { this.candidateExamId = value; }
  public Integer getResultRevisionNo() { return resultRevisionNo; }
  public void setResultRevisionNo(Integer value) { this.resultRevisionNo = value; }
  public String getTaskStatus() { return taskStatus; }
  public void setTaskStatus(String value) { this.taskStatus = value; }
  public Integer getAttemptCount() { return attemptCount; }
  public void setAttemptCount(Integer value) { this.attemptCount = value; }
  public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
  public void setNextAttemptAt(LocalDateTime value) { this.nextAttemptAt = value; }
  public String getLeaseOwner() { return leaseOwner; }
  public void setLeaseOwner(String value) { this.leaseOwner = value; }
  public LocalDateTime getLeaseUntil() { return leaseUntil; }
  public void setLeaseUntil(LocalDateTime value) { this.leaseUntil = value; }
  public String getLastError() { return lastError; }
  public void setLastError(String value) { this.lastError = value; }
  public String getRequestedReason() { return requestedReason; }
  public void setRequestedReason(String value) { this.requestedReason = value; }
  public Long getRequestedBy() { return requestedBy; }
  public void setRequestedBy(Long value) { this.requestedBy = value; }
  public LocalDateTime getStartedAt() { return startedAt; }
  public void setStartedAt(LocalDateTime value) { this.startedAt = value; }
  public LocalDateTime getCompletedAt() { return completedAt; }
  public void setCompletedAt(LocalDateTime value) { this.completedAt = value; }
}
