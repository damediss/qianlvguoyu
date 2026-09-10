package com.guoyu.platform.competition.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompetitionMakeExamPlanQueryReq extends PageQuery {

  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String contentType;
  private String status;
  private String keyword;

  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
