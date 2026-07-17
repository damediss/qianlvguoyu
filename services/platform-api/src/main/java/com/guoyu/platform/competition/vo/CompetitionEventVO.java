package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

/** Contract-only response skeleton for FND-02; entities are not exposed directly. */
public class CompetitionEventVO {

  private Long id;
  private String name;
  private String code;
  private String scope;
  private Long ownerOrgId;
  private String status;
  private Integer registrationAuditRequired;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private String description;
  private Long createdBy;
  private java.time.LocalDateTime createdAt;
  private Long updatedBy;
  private java.time.LocalDateTime updatedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getScope() { return scope; }
  public void setScope(String scope) { this.scope = scope; }
  public Long getOwnerOrgId() { return ownerOrgId; }
  public void setOwnerOrgId(Long ownerOrgId) { this.ownerOrgId = ownerOrgId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getRegistrationAuditRequired() { return registrationAuditRequired; }
  public void setRegistrationAuditRequired(Integer registrationAuditRequired) { this.registrationAuditRequired = registrationAuditRequired; }
  public LocalDateTime getStartAt() { return startAt; }
  public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
  public LocalDateTime getEndAt() { return endAt; }
  public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public java.time.LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
