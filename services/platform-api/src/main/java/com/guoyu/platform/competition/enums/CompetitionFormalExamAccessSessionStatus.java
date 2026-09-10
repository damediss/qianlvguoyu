package com.guoyu.platform.competition.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CompetitionFormalExamAccessSessionStatus {
  ACTIVE("active"),
  KICKED("kicked"),
  LOGGED_OUT("logged_out"),
  EXPIRED("expired"),
  REPLACED("replaced");

  private static final Map<String, CompetitionFormalExamAccessSessionStatus> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
          CompetitionFormalExamAccessSessionStatus::getCode, Function.identity()));

  private final String code;

  CompetitionFormalExamAccessSessionStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public boolean isActive() {
    return this == ACTIVE;
  }

  public static CompetitionFormalExamAccessSessionStatus fromCode(String code) {
    return code == null ? null : BY_CODE.get(code.trim().toLowerCase());
  }
}
