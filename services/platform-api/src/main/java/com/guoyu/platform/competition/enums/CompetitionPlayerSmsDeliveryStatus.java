package com.guoyu.platform.competition.enums;

public enum CompetitionPlayerSmsDeliveryStatus {
  PENDING("pending"),
  ACCEPTED("accepted"),
  FAILED("failed");

  private final String code;

  CompetitionPlayerSmsDeliveryStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
