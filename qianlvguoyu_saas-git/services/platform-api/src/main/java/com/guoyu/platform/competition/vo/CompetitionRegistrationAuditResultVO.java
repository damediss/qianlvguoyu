package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public class CompetitionRegistrationAuditResultVO {

  private Long registrationId;
  private Long eventId;
  private Long orgId;
  private Long rosterItemId;
  private String action;
  private String beforeStatus;
  private String status;
  private Long auditedBy;
  private LocalDateTime auditedAt;

  public Long getRegistrationId() {
    return registrationId;
  }

  public void setRegistrationId(Long registrationId) {
    this.registrationId = registrationId;
  }

  public Long getEventId() {
    return eventId;
  }

  public void setEventId(Long eventId) {
    this.eventId = eventId;
  }

  public Long getOrgId() {
    return orgId;
  }

  public void setOrgId(Long orgId) {
    this.orgId = orgId;
  }

  public Long getRosterItemId() {
    return rosterItemId;
  }

  public void setRosterItemId(Long rosterItemId) {
    this.rosterItemId = rosterItemId;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getBeforeStatus() {
    return beforeStatus;
  }

  public void setBeforeStatus(String beforeStatus) {
    this.beforeStatus = beforeStatus;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getAuditedBy() {
    return auditedBy;
  }

  public void setAuditedBy(Long auditedBy) {
    this.auditedBy = auditedBy;
  }

  public LocalDateTime getAuditedAt() {
    return auditedAt;
  }

  public void setAuditedAt(LocalDateTime auditedAt) {
    this.auditedAt = auditedAt;
  }
}
