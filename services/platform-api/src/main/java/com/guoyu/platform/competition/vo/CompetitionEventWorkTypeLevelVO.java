package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public class CompetitionEventWorkTypeLevelVO {

  private Long id;
  private Long eventId;
  private Long eventWorkTypeId;
  private String levelCode;
  private String levelName;
  private String sourceLevelCode;
  private String sourceLevelText;
  private String reconciliationStatus;
  private LocalDateTime confirmedAt;
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
  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
  public Long getConfirmedBy() { return confirmedBy; }
  public void setConfirmedBy(Long confirmedBy) { this.confirmedBy = confirmedBy; }
}
