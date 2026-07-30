package com.guoyu.platform.competition.enums;

public enum CompetitionScoreRuleStatus {
  DRAFT("draft"),
  PUBLISHED("published"),
  DEPRECATED("deprecated");

  private final String code;

  CompetitionScoreRuleStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
