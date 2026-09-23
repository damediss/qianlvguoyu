package com.guoyu.platform.competition.domain;

import java.util.Collection;
import java.util.Objects;

/**
 * 裁判小组同一时间只能承担一场赛事的裁判任务，但可以参加该赛事的多个赛段。
 */
public final class CompetitionRefereeGroupScheduleGuard {

  private CompetitionRefereeGroupScheduleGuard() {
  }

  public static boolean occupiesDifferentEvent(Long currentEventId, Collection<Long> occupiedEventIds) {
    if (currentEventId == null || occupiedEventIds == null || occupiedEventIds.isEmpty()) {
      return false;
    }
    return occupiedEventIds.stream()
        .filter(Objects::nonNull)
        .anyMatch(eventId -> !Objects.equals(eventId, currentEventId));
  }
}
