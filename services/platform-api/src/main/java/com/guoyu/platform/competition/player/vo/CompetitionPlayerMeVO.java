package com.guoyu.platform.competition.player.vo;

import java.util.List;

public record CompetitionPlayerMeVO(Long playerId, String status, List<EventSummary> events) {
  public record EventSummary(Long eventId, String eventName, String registrationStatus) {
  }
}
