package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/** One deterministic check shown before an event can be published. */
public record CompetitionEventPublishReadinessCheckVO(
    String code,
    boolean passed,
    String message,
    String targetType,
    @JsonSerialize(using = ToStringSerializer.class) Long targetId) {

  public CompetitionEventPublishReadinessCheckVO(String code, boolean passed, String message) {
    this(code, passed, message, null, null);
  }
}
