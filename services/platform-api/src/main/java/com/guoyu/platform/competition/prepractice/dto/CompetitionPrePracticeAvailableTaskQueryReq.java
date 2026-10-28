package com.guoyu.platform.competition.prepractice.dto;

import com.guoyu.platform.common.api.PageQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CompetitionPrePracticeAvailableTaskQueryReq extends PageQuery {
  @NotNull private Long eventId;
  @NotNull private Long eventWorkTypeId;
  @NotBlank private String levelCode;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getLevelCode() { return levelCode; }
  public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
}
