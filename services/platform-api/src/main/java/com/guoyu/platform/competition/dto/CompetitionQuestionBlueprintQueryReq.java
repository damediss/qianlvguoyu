package com.guoyu.platform.competition.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompetitionQuestionBlueprintQueryReq extends PageQuery {
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String contentType;
  private String status;

  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { this.eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { this.eventWorkTypeLevelId = value; }
  public String getContentType() { return contentType; }
  public void setContentType(String value) { this.contentType = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { this.status = value; }
}
