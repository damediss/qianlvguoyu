package com.guoyu.platform.competition.enums;

public enum CompetitionPlayerStatus {
  PENDING_ACTIVATION("pending_activation"),
  ACTIVE("active"),
  DISABLED("disabled");

  private final String code;

  CompetitionPlayerStatus(String code) { this.code = code; }
  public String getCode() { return code; }
}
