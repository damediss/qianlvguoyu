package com.guoyu.platform.competition.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CompetitionFormalCandidateExamStatus {
  WAITING_ENTER("waiting_enter"),
  READY("ready"),
  RUNNING("running"),
  SUBMITTED("submitted"),
  AUTO_SUBMITTED("auto_submitted"),
  FORCED_SUBMITTED("forced_submitted"),
  ABSENT("absent");

  private static final Map<String, CompetitionFormalCandidateExamStatus> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
          CompetitionFormalCandidateExamStatus::getCode, Function.identity()));

  private final String code;

  CompetitionFormalCandidateExamStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public boolean isTerminal() {
    return this == SUBMITTED || this == AUTO_SUBMITTED || this == FORCED_SUBMITTED;
  }

  public boolean isClosed() {
    return isTerminal() || this == ABSENT;
  }

  public static CompetitionFormalCandidateExamStatus fromCode(String code) {
    return code == null ? null : BY_CODE.get(code.trim().toLowerCase());
  }
}
