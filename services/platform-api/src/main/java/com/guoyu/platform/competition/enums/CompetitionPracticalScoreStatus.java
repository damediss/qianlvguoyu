package com.guoyu.platform.competition.enums;

public enum CompetitionPracticalScoreStatus {
  ASSIGNED("assigned"),
  DRAFT("draft"),
  SUBMITTED_LOCKED("submitted_locked"),
  UNLOCKED_FOR_RESCORE("unlocked_for_rescore"),
  RECUSED("recused"),
  VOIDED("voided");

  private final String code;

  CompetitionPracticalScoreStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
