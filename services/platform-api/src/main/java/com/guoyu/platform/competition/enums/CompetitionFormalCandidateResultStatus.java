package com.guoyu.platform.competition.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CompetitionFormalCandidateResultStatus {
  SCORED("scored"),
  ABSENT("absent");

  private static final Map<String, CompetitionFormalCandidateResultStatus> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
          CompetitionFormalCandidateResultStatus::getCode, Function.identity()));

  private final String code;

  CompetitionFormalCandidateResultStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static CompetitionFormalCandidateResultStatus fromCode(String code) {
    return code == null ? null : BY_CODE.get(code.trim().toLowerCase());
  }
}
