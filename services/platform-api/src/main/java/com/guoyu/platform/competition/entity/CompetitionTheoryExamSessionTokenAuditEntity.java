package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_theory_exam_session_token_audit")
public class CompetitionTheoryExamSessionTokenAuditEntity extends BaseEntity {
  private Long playerId;
  private Long assignmentId;
  private Long attemptId;
  private String tokenFingerprint;
  private String eventType;
  private String sessionStatus;
  private String reason;
  private Long operatedBy;
  private LocalDateTime occurredAt;
  private String requestId;

  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public Long getAssignmentId() { return assignmentId; }
  public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
  public Long getAttemptId() { return attemptId; }
  public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
  public String getTokenFingerprint() { return tokenFingerprint; }
  public void setTokenFingerprint(String tokenFingerprint) { this.tokenFingerprint = tokenFingerprint; }
  public String getEventType() { return eventType; }
  public void setEventType(String eventType) { this.eventType = eventType; }
  public String getSessionStatus() { return sessionStatus; }
  public void setSessionStatus(String sessionStatus) { this.sessionStatus = sessionStatus; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public Long getOperatedBy() { return operatedBy; }
  public void setOperatedBy(Long operatedBy) { this.operatedBy = operatedBy; }
  public LocalDateTime getOccurredAt() { return occurredAt; }
  public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
  public String getRequestId() { return requestId; }
  public void setRequestId(String requestId) { this.requestId = requestId; }
}
