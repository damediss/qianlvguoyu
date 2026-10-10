package com.guoyu.platform.competition.enums;

public enum CompetitionNoticeStatus {
  DRAFT("draft"), PUBLISHED("published");

  private final String code;

  CompetitionNoticeStatus(String code) { this.code = code; }
  public String getCode() { return code; }
}
