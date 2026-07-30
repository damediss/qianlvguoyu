package com.guoyu.platform.competition.enums;

public enum CompetitionRosterImportStatus {
  VALIDATING("validating"), VALIDATION_FAILED("validation_failed"),
  PENDING_CONFIRM("pending_confirm"), CONFIRMED("confirmed");

  private final String code;

  CompetitionRosterImportStatus(String code) { this.code = code; }
  public String getCode() { return code; }
}
