package com.guoyu.platform.competition.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompetitionRegistrationQueryReq extends PageQuery {

  private Long eventId;
  private Long orgId;
  private String status;
  private String name;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
}
