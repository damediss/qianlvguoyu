package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CompetitionTimeRangeOverlapTest {

  @Test
  void overlappingWindowsAreConflictsWhileTouchingEndpointsAreNot() {
    LocalDateTime morningStart = LocalDateTime.of(2026, 9, 8, 8, 0);
    LocalDateTime morningEnd = LocalDateTime.of(2026, 9, 8, 12, 0);
    LocalDateTime noonStart = LocalDateTime.of(2026, 9, 8, 11, 0);
    LocalDateTime afternoonStart = LocalDateTime.of(2026, 9, 8, 12, 0);
    LocalDateTime afternoonEnd = LocalDateTime.of(2026, 9, 8, 16, 0);

    assertThat(CompetitionTimeRangeOverlap.overlaps(morningStart, morningEnd, noonStart, afternoonEnd))
        .isTrue();
    assertThat(CompetitionTimeRangeOverlap.overlaps(morningStart, morningEnd, afternoonStart, afternoonEnd))
        .isFalse();
    assertThat(CompetitionTimeRangeOverlap.complete(morningStart, morningEnd)).isTrue();
    assertThat(CompetitionTimeRangeOverlap.complete(morningStart, null)).isFalse();
  }
}
