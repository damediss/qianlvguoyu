package com.guoyu.platform.competition.player.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.util.List;

public record CompetitionPlayerMeVO(
    @JsonSerialize(using = ToStringSerializer.class) Long playerId,
    String status,
    String maskedMobile,
    List<EventSummary> events) {
  public record EventSummary(
      @JsonSerialize(using = ToStringSerializer.class) Long eventId,
      String eventName,
      String registrationStatus) {
  }
}
