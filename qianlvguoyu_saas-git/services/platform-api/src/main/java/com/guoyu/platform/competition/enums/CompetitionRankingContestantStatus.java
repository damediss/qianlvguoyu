package com.guoyu.platform.competition.enums;

public enum CompetitionRankingContestantStatus {
  COMPLETED("completed"),
  ABSENT("absent");

  private final String code;

  CompetitionRankingContestantStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
