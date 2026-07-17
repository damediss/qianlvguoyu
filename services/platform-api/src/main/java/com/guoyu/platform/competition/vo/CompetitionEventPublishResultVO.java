package com.guoyu.platform.competition.vo;

public class CompetitionEventPublishResultVO {

  private Long eventId;
  private String eventCode;
  private String eventScope;
  private Long ownerOrgId;
  private String previousStatus;
  private String status;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public String getEventCode() { return eventCode; }
  public void setEventCode(String eventCode) { this.eventCode = eventCode; }
  public String getEventScope() { return eventScope; }
  public void setEventScope(String eventScope) { this.eventScope = eventScope; }
  public Long getOwnerOrgId() { return ownerOrgId; }
  public void setOwnerOrgId(Long ownerOrgId) { this.ownerOrgId = ownerOrgId; }
  public String getPreviousStatus() { return previousStatus; }
  public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
