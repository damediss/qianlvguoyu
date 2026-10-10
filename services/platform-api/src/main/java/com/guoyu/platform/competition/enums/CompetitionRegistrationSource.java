package com.guoyu.platform.competition.enums;

public enum CompetitionRegistrationSource {
  ENTERPRISE_IMPORT("enterprise_import");

  private final String code;

  CompetitionRegistrationSource(String code) { this.code = code; }
  public String getCode() { return code; }
}
