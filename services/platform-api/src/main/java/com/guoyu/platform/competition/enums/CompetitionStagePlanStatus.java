package com.guoyu.platform.competition.enums;

/** Lifecycle of the event-level stage arrangement. */
public enum CompetitionStagePlanStatus {
  DRAFT("draft"),
  CONFIRMED("confirmed");

  private final String code;

  CompetitionStagePlanStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
