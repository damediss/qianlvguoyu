package com.guoyu.platform.competition.enums;

public enum CompetitionScoreSourceType {
  THEORY("theory"),
  SIMULATION("simulation"),
  PRACTICAL("practical");

  private final String code;

  CompetitionScoreSourceType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
