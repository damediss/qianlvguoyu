package com.guoyu.platform.competition.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompetitionStageQueryReq extends PageQuery {

  private String status;
  private Long eventWorkTypeId;
  private String eventWorkTypeLevelCode;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getEventWorkTypeId() {
    return eventWorkTypeId;
  }

  public void setEventWorkTypeId(Long eventWorkTypeId) {
    this.eventWorkTypeId = eventWorkTypeId;
  }

  public String getEventWorkTypeLevelCode() {
    return eventWorkTypeLevelCode;
  }

  public void setEventWorkTypeLevelCode(String eventWorkTypeLevelCode) {
    this.eventWorkTypeLevelCode = eventWorkTypeLevelCode;
  }
}
