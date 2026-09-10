package com.guoyu.platform.competition.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CompetitionFormalExamType {
  THEORY("theory"),
  PRACTICAL("practical"),
  SIMULATION("simulation");

  private static final Map<String, CompetitionFormalExamType> BY_CODE = Arrays.stream(values())
      .collect(Collectors.toUnmodifiableMap(CompetitionFormalExamType::getCode, Function.identity()));

  private final String code;

  CompetitionFormalExamType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static CompetitionFormalExamType fromCode(String code) {
    if (code == null || code.isBlank()) {
      return THEORY;
    }
    return BY_CODE.get(code.trim().toLowerCase());
  }
}
