package com.guoyu.platform.competition.enums;

public enum CompetitionScoreConfirmationStatus {
  PENDING("pending"),
  CONFIRMED("confirmed"),
  VOIDED("voided");

  private final String code;

  CompetitionScoreConfirmationStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
