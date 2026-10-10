package com.guoyu.platform.competition.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CompetitionFormalExamScoringStatus {
  NOT_STARTED("not_started"),
  QUEUED("queued"),
  SCORING("scoring"),
  COMPLETED("completed"),
  FAILED("failed");

  private static final Map<String, CompetitionFormalExamScoringStatus> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
          CompetitionFormalExamScoringStatus::getCode, Function.identity()));

  private final String code;

  CompetitionFormalExamScoringStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static CompetitionFormalExamScoringStatus fromCode(String code) {
    return code == null ? null : BY_CODE.get(code.trim().toLowerCase());
  }
}
