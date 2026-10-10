package com.guoyu.platform.competition.vo;

public class CompetitionEventOrgVO {

  private Long id;
  private Long eventId;
  private Long orgId;
  private String orgName;
  private String orgCode;
  private Integer quota;
  private String status;
  private Integer importPermission;
  private java.time.LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public String getOrgCode() { return orgCode; }
  public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
  public Integer getQuota() { return quota; }
  public void setQuota(Integer quota) { this.quota = quota; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getImportPermission() { return importPermission; }
  public void setImportPermission(Integer importPermission) { this.importPermission = importPermission; }
  public java.time.LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
}
