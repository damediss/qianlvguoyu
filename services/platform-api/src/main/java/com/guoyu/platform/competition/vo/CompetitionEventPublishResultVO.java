package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class CompetitionEventPublishResultVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventId;
  private String eventCode;
  private String playerEntryCode;
  private String eventScope;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long ownerOrgId;
  private String previousStatus;
  private String status;
  private String stagePlanStatus;
  private Integer activeWorkTypeCount;
  private Integer selectedLevelCount;
  private Integer confirmedScopeCount;
  private Integer stageCount;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public String getEventCode() { return eventCode; }
  public void setEventCode(String eventCode) { this.eventCode = eventCode; }
  public String getPlayerEntryCode() { return playerEntryCode; }
  public void setPlayerEntryCode(String playerEntryCode) { this.playerEntryCode = playerEntryCode; }
  public String getEventScope() { return eventScope; }
  public void setEventScope(String eventScope) { this.eventScope = eventScope; }
  public Long getOwnerOrgId() { return ownerOrgId; }
  public void setOwnerOrgId(Long ownerOrgId) { this.ownerOrgId = ownerOrgId; }
  public String getPreviousStatus() { return previousStatus; }
  public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getStagePlanStatus() { return stagePlanStatus; }
  public void setStagePlanStatus(String stagePlanStatus) { this.stagePlanStatus = stagePlanStatus; }
  public Integer getActiveWorkTypeCount() { return activeWorkTypeCount; }
  public void setActiveWorkTypeCount(Integer activeWorkTypeCount) { this.activeWorkTypeCount = activeWorkTypeCount; }
  public Integer getSelectedLevelCount() { return selectedLevelCount; }
  public void setSelectedLevelCount(Integer selectedLevelCount) { this.selectedLevelCount = selectedLevelCount; }
  public Integer getConfirmedScopeCount() { return confirmedScopeCount; }
  public void setConfirmedScopeCount(Integer confirmedScopeCount) { this.confirmedScopeCount = confirmedScopeCount; }
  public Integer getStageCount() { return stageCount; }
  public void setStageCount(Integer stageCount) { this.stageCount = stageCount; }
}
