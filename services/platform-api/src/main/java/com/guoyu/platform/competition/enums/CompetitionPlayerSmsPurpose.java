package com.guoyu.platform.competition.enums;

public enum CompetitionPlayerSmsPurpose {
  ACTIVATION("activation"),
  LOGIN("login"),
  CHANGE_MOBILE_OLD("change_mobile_old"),
  CHANGE_MOBILE_NEW("change_mobile_new");

  private final String code;

  CompetitionPlayerSmsPurpose(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
