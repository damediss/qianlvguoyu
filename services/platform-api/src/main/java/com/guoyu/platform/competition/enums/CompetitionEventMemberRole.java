package com.guoyu.platform.competition.enums;

public enum CompetitionEventMemberRole {
  CONTESTANT("contestant"),
  EXPERT("expert"),
  REFEREE("referee"),
  EVENT_ADMIN("event_admin"),
  STAFF("staff");

  private final String code;

  CompetitionEventMemberRole(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
