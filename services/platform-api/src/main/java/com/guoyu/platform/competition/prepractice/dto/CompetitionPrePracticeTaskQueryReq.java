package com.guoyu.platform.competition.prepractice.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompetitionPrePracticeTaskQueryReq extends PageQuery {

  private Long eventId;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
}
