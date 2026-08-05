package com.guoyu.platform.competition.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Server-owned seven-state lifecycle for a competition stage. */
public enum CompetitionStageStatus {
  DRAFT("draft"),
  CONFIGURING("configuring"),
  READY_LOCKED("ready_locked"),
  IN_PROGRESS("in_progress"),
  CLOSED("closed"),
  SETTLED("settled"),
  RESULT_PUBLISHED("result_published");

  private static final Map<String, CompetitionStageStatus> BY_CODE = java.util.Arrays.stream(values())
      .collect(Collectors.toUnmodifiableMap(CompetitionStageStatus::getCode, Function.identity()));

  private final String code;

  CompetitionStageStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static CompetitionStageStatus fromCode(String code) {
    return code == null ? null : BY_CODE.get(code.trim().toLowerCase());
  }

  public static Set<CompetitionStageStatus> writableStatuses() {
    return EnumSet.of(DRAFT, CONFIGURING);
  }
}
