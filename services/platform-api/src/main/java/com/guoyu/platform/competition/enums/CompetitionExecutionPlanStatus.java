package com.guoyu.platform.competition.enums;

public enum CompetitionExecutionPlanStatus {
  DRAFT("draft"),
  PUBLISHED("published"),
  WITHDRAWN("withdrawn");

  private final String code;

  CompetitionExecutionPlanStatus(String code) {
    this.code = code;
  }

  public String getCode() { return code; }

  public static CompetitionExecutionPlanStatus fromCode(String code) {
    if (code == null) return null;
    for (CompetitionExecutionPlanStatus value : values()) {
      if (value.code.equalsIgnoreCase(code.trim())) return value;
    }
    return null;
  }
}
