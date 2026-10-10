package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_formal_exam_outbox")
public class CompetitionFormalExamOutboxEntity extends BaseEntity {

  private String eventId;
  private String eventType;
  private Long aggregateId;
  private Long examSessionId;
  private Long candidateExamId;
  private Long accessSessionId;
  private Integer generation;
  private Long seq;
  private String payloadJson;
  private String status;
  private Integer attempts;
  private LocalDateTime publishedAt;
  private String lastError;

  public String getEventId() { return eventId; }
  public void setEventId(String eventId) { this.eventId = eventId; }
  public String getEventType() { return eventType; }
  public void setEventType(String eventType) { this.eventType = eventType; }
  public Long getAggregateId() { return aggregateId; }
  public void setAggregateId(Long aggregateId) { this.aggregateId = aggregateId; }
  public Long getExamSessionId() { return examSessionId; }
  public void setExamSessionId(Long examSessionId) { this.examSessionId = examSessionId; }
  public Long getCandidateExamId() { return candidateExamId; }
  public void setCandidateExamId(Long candidateExamId) { this.candidateExamId = candidateExamId; }
  public Long getAccessSessionId() { return accessSessionId; }
  public void setAccessSessionId(Long accessSessionId) { this.accessSessionId = accessSessionId; }
  public Integer getGeneration() { return generation; }
  public void setGeneration(Integer generation) { this.generation = generation; }
  public Long getSeq() { return seq; }
  public void setSeq(Long seq) { this.seq = seq; }
  public String getPayloadJson() { return payloadJson; }
  public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getAttempts() { return attempts; }
  public void setAttempts(Integer attempts) { this.attempts = attempts; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
  public String getLastError() { return lastError; }
  public void setLastError(String lastError) { this.lastError = lastError; }
}
