package com.guoyu.platform.competition.vo;

public class CompetitionWorkTypeCandidateVO {

  private Long eventWorkTypeId;
  private Long workTypeId;
  private String displayName;

  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) {
    this.eventWorkTypeId = eventWorkTypeId;
  }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
}
