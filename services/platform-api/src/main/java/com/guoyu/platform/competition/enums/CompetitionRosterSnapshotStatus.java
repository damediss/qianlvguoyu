package com.guoyu.platform.competition.enums;

public enum CompetitionRosterSnapshotStatus {
  DRAFT("draft"), PUBLISHED("published"), SUPERSEDED("superseded");

  private final String code;

  CompetitionRosterSnapshotStatus(String code) { this.code = code; }
  public String getCode() { return code; }
}
