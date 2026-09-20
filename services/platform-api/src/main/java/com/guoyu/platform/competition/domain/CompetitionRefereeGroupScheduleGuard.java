package com.guoyu.platform.competition.domain;

import java.time.LocalDateTime;

/**
 * 裁判小组高于赛段：同一小组不能被时间重叠的赛段同时调用。
 */
public final class CompetitionRefereeGroupScheduleGuard {

  private CompetitionRefereeGroupScheduleGuard() {
  }

  public static boolean overlaps(
      LocalDateTime leftStart,
      LocalDateTime leftEnd,
      LocalDateTime rightStart,
      LocalDateTime rightEnd) {
    if (leftStart == null || leftEnd == null || rightStart == null || rightEnd == null) {
      return false;
    }
    return leftStart.isBefore(rightEnd) && rightStart.isBefore(leftEnd);
  }
}
