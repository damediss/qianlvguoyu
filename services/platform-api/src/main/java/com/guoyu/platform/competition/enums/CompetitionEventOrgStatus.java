package com.guoyu.platform.competition.enums;

public enum CompetitionEventOrgStatus {
  INVITED("invited"), ACTIVE("active"), SUSPENDED("suspended"), CLOSED("closed");

  private final String code;

  CompetitionEventOrgStatus(String code) { this.code = code; }
  public String getCode() { return code; }
}
