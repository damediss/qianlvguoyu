package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_formal_exam_access_command")
public class CompetitionFormalExamAccessCommandEntity extends BaseEntity {

  private Long examSessionId;
  private Long candidateExamId;
  private Long accessSessionId;
  private Integer generation;
  private String commandType;
  private String reason;
  private Long operatorId;
  private String status;
  private LocalDateTime ackAt;
  private LocalDateTime expiresAt;

  public Long getExamSessionId() { return examSessionId; }
  public void setExamSessionId(Long examSessionId) { this.examSessionId = examSessionId; }
  public Long getCandidateExamId() { return candidateExamId; }
  public void setCandidateExamId(Long candidateExamId) { this.candidateExamId = candidateExamId; }
  public Long getAccessSessionId() { return accessSessionId; }
  public void setAccessSessionId(Long accessSessionId) { this.accessSessionId = accessSessionId; }
  public Integer getGeneration() { return generation; }
  public void setGeneration(Integer generation) { this.generation = generation; }
  public String getCommandType() { return commandType; }
  public void setCommandType(String commandType) { this.commandType = commandType; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public Long getOperatorId() { return operatorId; }
  public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getAckAt() { return ackAt; }
  public void setAckAt(LocalDateTime ackAt) { this.ackAt = ackAt; }
  public LocalDateTime getExpiresAt() { return expiresAt; }
  public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
