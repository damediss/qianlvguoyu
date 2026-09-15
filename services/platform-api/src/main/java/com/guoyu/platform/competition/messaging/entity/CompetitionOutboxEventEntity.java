package com.guoyu.platform.competition.messaging.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_outbox_event")
public class CompetitionOutboxEventEntity extends BaseEntity {

  private String eventChannel;
  private String aggregateType;
  private Long aggregateId;
  private String eventType;
  private String routingKey;
  private String eventKey;
  private String payloadJson;
  private String status;
  private Integer attemptCount;
  private LocalDateTime nextAttemptAt;
  private LocalDateTime publishedAt;
  private String lastError;

  public String getEventChannel() { return eventChannel; }
  public void setEventChannel(String eventChannel) { this.eventChannel = eventChannel; }
  public String getAggregateType() { return aggregateType; }
  public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
  public Long getAggregateId() { return aggregateId; }
  public void setAggregateId(Long aggregateId) { this.aggregateId = aggregateId; }
  public String getEventType() { return eventType; }
  public void setEventType(String eventType) { this.eventType = eventType; }
  public String getRoutingKey() { return routingKey; }
  public void setRoutingKey(String routingKey) { this.routingKey = routingKey; }
  public String getEventKey() { return eventKey; }
  public void setEventKey(String eventKey) { this.eventKey = eventKey; }
  public String getPayloadJson() { return payloadJson; }
  public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getAttemptCount() { return attemptCount; }
  public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }
  public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
  public void setNextAttemptAt(LocalDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
  public LocalDateTime getPublishedAt() { return publishedAt; }
  public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
  public String getLastError() { return lastError; }
  public void setLastError(String lastError) { this.lastError = lastError; }
}
