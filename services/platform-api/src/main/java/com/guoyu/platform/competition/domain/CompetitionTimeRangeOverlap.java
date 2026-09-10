package com.guoyu.platform.competition.domain;

import java.time.LocalDateTime;

/** Inclusive-open overlap: adjacent windows that only touch at an endpoint are not conflicts. */
public final class CompetitionTimeRangeOverlap {

  private CompetitionTimeRangeOverlap() {
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

  public static boolean complete(LocalDateTime start, LocalDateTime end) {
    return start != null && end != null && !end.isBefore(start);
  }
}
