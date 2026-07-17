package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public class CompetitionStageVO {

  private Long id;
  private Long eventId;
  private String name;
  private String stageType;
  private Integer sortOrder;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private String status;
  private String description;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
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
}
