package com.guoyu.platform.competition.enums;

public enum CompetitionRosterValidationStatus {
  VALID("valid"), INVALID("invalid");

  private final String code;

  CompetitionRosterValidationStatus(String code) { this.code = code; }
  public String getCode() { return code; }
}
