package com.guoyu.platform.competition.enums;

public enum CompetitionEventScope {
  PLATFORM("platform"),
  ENTERPRISE_INTERNAL("enterprise_internal");

  private final String code;

  CompetitionEventScope(String code) { this.code = code; }
  public String getCode() { return code; }
}
