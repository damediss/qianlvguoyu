package com.guoyu.platform.competition.prepractice.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompetitionPrePracticeDispatchQueryReq extends PageQuery {
  private Long eventId;
  private Long eventWorkTypeId;
  private String levelCode;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getLevelCode() { return levelCode; }
  public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
}
