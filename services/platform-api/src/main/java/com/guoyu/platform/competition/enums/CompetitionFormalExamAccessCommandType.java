package com.guoyu.platform.competition.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CompetitionFormalExamAccessCommandType {
  FORCE_OFFLINE("force_offline"),
  LOGIN_REPLACE("login_replace"),
  LOGOUT("logout");

  private static final Map<String, CompetitionFormalExamAccessCommandType> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
          CompetitionFormalExamAccessCommandType::getCode, Function.identity()));

  private final String code;

  CompetitionFormalExamAccessCommandType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static CompetitionFormalExamAccessCommandType fromCode(String code) {
    return code == null ? null : BY_CODE.get(code.trim().toLowerCase());
  }
}
