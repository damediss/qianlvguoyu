package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_event")
public class CompetitionEventEntity extends BaseEntity {

  private String name;
  private String code;
  private String playerEntryCode;
  private String scope;
  private Long ownerOrgId;
  private String status;
  private String stagePlanStatus;
  private Long stagePlanConfirmedBy;
  private LocalDateTime stagePlanConfirmedAt;
  private Integer registrationAuditRequired;
  private LocalDateTime startAt;
  private LocalDateTime endAt;
  private String description;

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getPlayerEntryCode() { return playerEntryCode; }
  public void setPlayerEntryCode(String playerEntryCode) { this.playerEntryCode = playerEntryCode; }
  public String getScope() { return scope; }
  public void setScope(String scope) { this.scope = scope; }
  public Long getOwnerOrgId() { return ownerOrgId; }
  public void setOwnerOrgId(Long ownerOrgId) { this.ownerOrgId = ownerOrgId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getStagePlanStatus() { return stagePlanStatus; }
  public void setStagePlanStatus(String stagePlanStatus) { this.stagePlanStatus = stagePlanStatus; }
  public Long getStagePlanConfirmedBy() { return stagePlanConfirmedBy; }
  public void setStagePlanConfirmedBy(Long stagePlanConfirmedBy) { this.stagePlanConfirmedBy = stagePlanConfirmedBy; }
  public LocalDateTime getStagePlanConfirmedAt() { return stagePlanConfirmedAt; }
  public void setStagePlanConfirmedAt(LocalDateTime stagePlanConfirmedAt) { this.stagePlanConfirmedAt = stagePlanConfirmedAt; }
  public Integer getRegistrationAuditRequired() { return registrationAuditRequired; }
  public void setRegistrationAuditRequired(Integer registrationAuditRequired) { this.registrationAuditRequired = registrationAuditRequired; }
  public LocalDateTime getStartAt() { return startAt; }
  public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
  public LocalDateTime getEndAt() { return endAt; }
  public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
}
