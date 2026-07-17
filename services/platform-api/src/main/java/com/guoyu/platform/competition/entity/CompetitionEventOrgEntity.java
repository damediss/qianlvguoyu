package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_event_org")
public class CompetitionEventOrgEntity extends BaseEntity {

  private Long eventId;
  private Long orgId;
  private Integer quota;
  private String status;
  private Integer importPermission;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public Integer getQuota() { return quota; }
  public void setQuota(Integer quota) { this.quota = quota; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getImportPermission() { return importPermission; }
  public void setImportPermission(Integer importPermission) { this.importPermission = importPermission; }
}
