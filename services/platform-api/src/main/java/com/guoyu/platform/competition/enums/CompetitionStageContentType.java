package com.guoyu.platform.competition.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CompetitionStageContentType {
  THEORY("theory"),
  SIMULATION("simulation"),
  PRACTICAL("practical");

  private static final Map<String, CompetitionStageContentType> BY_CODE = Arrays.stream(values())
      .collect(Collectors.toUnmodifiableMap(CompetitionStageContentType::getCode, Function.identity()));

  private final String code;

  CompetitionStageContentType(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static CompetitionStageContentType fromCode(String code) {
    return code == null ? null : BY_CODE.get(code.trim().toLowerCase());
  }
}
