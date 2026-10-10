package com.guoyu.platform.competition.enums;

public enum CompetitionUnlockRequestStatus {
  PENDING("pending"),
  APPROVED("approved"),
  REJECTED("rejected");

  private final String code;

  CompetitionUnlockRequestStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
