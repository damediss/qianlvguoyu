package com.guoyu.platform.competition.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Fixed stage types available to each independent work-type-level canvas. */
public enum CompetitionStageType {
  QUALIFICATION("qualification", "资格赛", 10),
  PRELIMINARY("preliminary", "初赛", 20),
  QUARTERFINAL("quarterfinal", "复赛", 30),
  SEMIFINAL("semifinal", "半决赛", 40),
  FINAL("final", "决赛", 50);

  private static final Map<String, CompetitionStageType> BY_CODE = Arrays.stream(values())
      .collect(Collectors.toUnmodifiableMap(CompetitionStageType::getCode, Function.identity()));

  private final String code;
  private final String displayName;
  private final int sortOrder;

  CompetitionStageType(String code, String displayName, int sortOrder) {
    this.code = code;
    this.displayName = displayName;
    this.sortOrder = sortOrder;
  }

  public String getCode() {
    return code;
  }

  public String getDisplayName() {
    return displayName;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public static CompetitionStageType fromCode(String code) {
    return code == null ? null : BY_CODE.get(code.trim().toLowerCase());
  }

  public static Comparator<CompetitionStageType> sortByOrder() {
    return Comparator.comparingInt(CompetitionStageType::getSortOrder);
  }
}
