package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public class CompetitionStageVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long id;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeId;
  private String eventWorkTypeLevelCode;
  private String name;
  private String stageType;
  private Integer sortOrder;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private String status;
  private String description;
  private LocalDateTime frozenAt;
  private LocalDateTime readyAt;
  private LocalDateTime closedAt;
  private LocalDateTime resultPublishedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getEventWorkTypeLevelCode() { return eventWorkTypeLevelCode; }
  public void setEventWorkTypeLevelCode(String eventWorkTypeLevelCode) {
    this.eventWorkTypeLevelCode = eventWorkTypeLevelCode;
  }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getStageType() { return stageType; }
  public void setStageType(String stageType) { this.stageType = stageType; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public LocalDateTime getStartAt() { return startAt; }
  public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
  public LocalDateTime getEndAt() { return endAt; }
  public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public LocalDateTime getFrozenAt() { return frozenAt; }
  public void setFrozenAt(LocalDateTime frozenAt) { this.frozenAt = frozenAt; }
  public LocalDateTime getReadyAt() { return readyAt; }
  public void setReadyAt(LocalDateTime readyAt) { this.readyAt = readyAt; }
  public LocalDateTime getClosedAt() { return closedAt; }
  public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
  public LocalDateTime getResultPublishedAt() { return resultPublishedAt; }
  public void setResultPublishedAt(LocalDateTime resultPublishedAt) { this.resultPublishedAt = resultPublishedAt; }
}
