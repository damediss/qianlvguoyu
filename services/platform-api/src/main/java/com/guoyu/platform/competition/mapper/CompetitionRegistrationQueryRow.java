package com.guoyu.platform.competition.mapper;

import java.time.LocalDateTime;

public class CompetitionRegistrationQueryRow {

  private Long registrationId;
  private Long eventId;
  private String eventName;
  private Long orgId;
  private String orgName;
  private Long rosterItemId;
  private String name;
  private String workType;
  private String level;
  private String source;
  private String status;
  private String auditReason;
  private Long auditedBy;
  private LocalDateTime auditedAt;
  private LocalDateTime createdAt;

  public Long getRegistrationId() { return registrationId; }
  public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public String getEventName() { return eventName; }
  public void setEventName(String eventName) { this.eventName = eventName; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public Long getRosterItemId() { return rosterItemId; }
  public void setRosterItemId(Long rosterItemId) { this.rosterItemId = rosterItemId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getWorkType() { return workType; }
  public void setWorkType(String workType) { this.workType = workType; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getAuditReason() { return auditReason; }
  public void setAuditReason(String auditReason) { this.auditReason = auditReason; }
  public Long getAuditedBy() { return auditedBy; }
  public void setAuditedBy(Long auditedBy) { this.auditedBy = auditedBy; }
  public LocalDateTime getAuditedAt() { return auditedAt; }
  public void setAuditedAt(LocalDateTime auditedAt) { this.auditedAt = auditedAt; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
