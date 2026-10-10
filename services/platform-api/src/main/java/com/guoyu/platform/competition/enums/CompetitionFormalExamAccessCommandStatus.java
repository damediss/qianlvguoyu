package com.guoyu.platform.competition.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CompetitionFormalExamAccessCommandStatus {
  ISSUED("issued"),
  ACKED("acked"),
  EXPIRED("expired");

  private static final Map<String, CompetitionFormalExamAccessCommandStatus> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
          CompetitionFormalExamAccessCommandStatus::getCode, Function.identity()));

  private final String code;

  CompetitionFormalExamAccessCommandStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static CompetitionFormalExamAccessCommandStatus fromCode(String code) {
    return code == null ? null : BY_CODE.get(code.trim().toLowerCase());
  }
}
