package com.guoyu.platform.competition.enums;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CompetitionFormalExamSessionStatus {
  PREPARING("preparing"),
  READY("ready"),
  RUNNING("running"),
  FINISHED("finished"),
  CANCELLED("cancelled");

  private static final Map<String, CompetitionFormalExamSessionStatus> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
          CompetitionFormalExamSessionStatus::getCode, Function.identity()));

  private final String code;

  CompetitionFormalExamSessionStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static CompetitionFormalExamSessionStatus fromCode(String code) {
    return code == null ? null : BY_CODE.get(code.trim().toLowerCase());
  }

  public static Set<CompetitionFormalExamSessionStatus> activeStatuses() {
    return EnumSet.of(PREPARING, READY, RUNNING);
  }

  public boolean isActive() {
    return activeStatuses().contains(this);
  }
}
