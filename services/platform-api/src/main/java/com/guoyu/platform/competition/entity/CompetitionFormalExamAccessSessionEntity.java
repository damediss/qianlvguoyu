package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_formal_exam_access_session")
public class CompetitionFormalExamAccessSessionEntity extends BaseEntity {

  private Long examSessionId;
  private Long candidateExamId;
  private Long playerId;
  private String deviceId;
  private Integer generation;
  private String status;
  private LocalDateTime loginAt;
  private LocalDateTime lastSeenAt;
  private LocalDateTime endedAt;
  private String endedReason;
  private String tokenFingerprint;

  public Long getExamSessionId() { return examSessionId; }
  public void setExamSessionId(Long examSessionId) { this.examSessionId = examSessionId; }
  public Long getCandidateExamId() { return candidateExamId; }
  public void setCandidateExamId(Long candidateExamId) { this.candidateExamId = candidateExamId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public String getDeviceId() { return deviceId; }
  public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
  public Integer getGeneration() { return generation; }
  public void setGeneration(Integer generation) { this.generation = generation; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getLoginAt() { return loginAt; }
  public void setLoginAt(LocalDateTime loginAt) { this.loginAt = loginAt; }
  public LocalDateTime getLastSeenAt() { return lastSeenAt; }
  public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
  public LocalDateTime getEndedAt() { return endedAt; }
  public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
  public String getEndedReason() { return endedReason; }
  public void setEndedReason(String endedReason) { this.endedReason = endedReason; }
  public String getTokenFingerprint() { return tokenFingerprint; }
  public void setTokenFingerprint(String tokenFingerprint) { this.tokenFingerprint = tokenFingerprint; }
}
