package com.guoyu.platform.competition.enums;

public enum CompetitionRefereeAssignmentRole {
  OFFICIAL("official"),
  STANDBY("standby");

  private final String code;

  CompetitionRefereeAssignmentRole(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
