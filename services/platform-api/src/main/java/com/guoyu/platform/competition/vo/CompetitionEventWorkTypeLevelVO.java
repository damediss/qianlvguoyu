package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public class CompetitionEventWorkTypeLevelVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long id;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeId;
  private String levelCode;
  private String levelName;
  private String sourceLevelCode;
  private String sourceLevelText;
  private String reconciliationStatus;
  private String stagePlanStatus;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long stagePlanConfirmedBy;
  private LocalDateTime stagePlanConfirmedAt;
  private LocalDateTime confirmedAt;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long confirmedBy;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getLevelCode() { return levelCode; }
  public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
  public String getLevelName() { return levelName; }
  public void setLevelName(String levelName) { this.levelName = levelName; }
  public String getSourceLevelCode() { return sourceLevelCode; }
  public void setSourceLevelCode(String sourceLevelCode) { this.sourceLevelCode = sourceLevelCode; }
  public String getSourceLevelText() { return sourceLevelText; }
  public void setSourceLevelText(String sourceLevelText) { this.sourceLevelText = sourceLevelText; }
  public String getReconciliationStatus() { return reconciliationStatus; }
  public void setReconciliationStatus(String reconciliationStatus) { this.reconciliationStatus = reconciliationStatus; }
  public String getStagePlanStatus() { return stagePlanStatus; }
  public void setStagePlanStatus(String stagePlanStatus) { this.stagePlanStatus = stagePlanStatus; }
  public Long getStagePlanConfirmedBy() { return stagePlanConfirmedBy; }
  public void setStagePlanConfirmedBy(Long stagePlanConfirmedBy) { this.stagePlanConfirmedBy = stagePlanConfirmedBy; }
  public LocalDateTime getStagePlanConfirmedAt() { return stagePlanConfirmedAt; }
  public void setStagePlanConfirmedAt(LocalDateTime stagePlanConfirmedAt) { this.stagePlanConfirmedAt = stagePlanConfirmedAt; }
  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
  public Long getConfirmedBy() { return confirmedBy; }
  public void setConfirmedBy(Long confirmedBy) { this.confirmedBy = confirmedBy; }
}
