package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_registration")
public class CompetitionRegistrationEntity extends BaseEntity {

  private Long eventId;
  private Long orgId;
  private Long rosterItemId;
  private Long playerId;
  private String source;
  private String status;
  private String auditReason;
  private Long auditedBy;
  private LocalDateTime auditedAt;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public Long getRosterItemId() { return rosterItemId; }
  public void setRosterItemId(Long rosterItemId) { this.rosterItemId = rosterItemId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
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
}
