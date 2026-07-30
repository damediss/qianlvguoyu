package com.guoyu.platform.competition.enums;

public enum CompetitionTheoryTemplate {
  THEORY_90,
  THEORY_120;

  public static boolean supports(String value) {
    if (value == null) {
      return false;
    }
    for (CompetitionTheoryTemplate template : values()) {
      if (template.name().equals(value)) {
        return true;
      }
    }
    return false;
  }
}
