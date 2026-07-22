package com.guoyu.platform.competition.enums;

public enum CompetitionPlayerSmsChallengeStatus {
  PENDING("pending"),
  VERIFIED("verified"),
  EXPIRED("expired"),
  LOCKED("locked"),
  CANCELLED("cancelled");

  private final String code;

  CompetitionPlayerSmsChallengeStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
