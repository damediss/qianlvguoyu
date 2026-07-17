package com.guoyu.platform.competition.enums;

public enum CompetitionEventStatus {
  DRAFT("draft"), PUBLISHED("published"), CLOSED("closed"), ARCHIVED("archived");

  private final String code;

  CompetitionEventStatus(String code) { this.code = code; }
  public String getCode() { return code; }
}
