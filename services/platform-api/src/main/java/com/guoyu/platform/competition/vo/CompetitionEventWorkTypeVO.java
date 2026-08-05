package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public class CompetitionEventWorkTypeVO {

  private Long id;
  private Long eventId;
  private Long workTypeId;
  private String workTypeName;
  private String displayName;
  private String status;
  private Integer sortOrder;
  private String stagePlanStatus;
  private Long stagePlanConfirmedBy;
  private LocalDateTime stagePlanConfirmedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public String getWorkTypeName() { return workTypeName; }
  public void setWorkTypeName(String workTypeName) { this.workTypeName = workTypeName; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public String getStagePlanStatus() { return stagePlanStatus; }
  public void setStagePlanStatus(String stagePlanStatus) { this.stagePlanStatus = stagePlanStatus; }
  public Long getStagePlanConfirmedBy() { return stagePlanConfirmedBy; }
  public void setStagePlanConfirmedBy(Long stagePlanConfirmedBy) { this.stagePlanConfirmedBy = stagePlanConfirmedBy; }
  public LocalDateTime getStagePlanConfirmedAt() { return stagePlanConfirmedAt; }
  public void setStagePlanConfirmedAt(LocalDateTime stagePlanConfirmedAt) { this.stagePlanConfirmedAt = stagePlanConfirmedAt; }
}
