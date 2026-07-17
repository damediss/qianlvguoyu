package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_event")
public class CompetitionEventEntity extends BaseEntity {

  private String name;
  private String code;
  private String scope;
  private Long ownerOrgId;
  private String status;
  private Integer registrationAuditRequired;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private String description;

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
}
