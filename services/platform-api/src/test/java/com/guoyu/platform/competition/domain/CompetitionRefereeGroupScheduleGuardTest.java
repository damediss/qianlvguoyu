package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CompetitionRefereeGroupScheduleGuardTest {

  @Test
  void overlappingStagesCannotShareAGroup() {
    LocalDateTime morningStart = LocalDateTime.of(2026, 9, 20, 9, 0);
    LocalDateTime morningEnd = LocalDateTime.of(2026, 9, 20, 12, 0);
    LocalDateTime noonStart = LocalDateTime.of(2026, 9, 20, 11, 0);
    LocalDateTime noonEnd = LocalDateTime.of(2026, 9, 20, 13, 0);

    assertThat(CompetitionRefereeGroupScheduleGuard.overlaps(
        morningStart, morningEnd, noonStart, noonEnd)).isTrue();
  }

  @Test
  void sequentialStagesCanReuseAGroup() {
    LocalDateTime morningStart = LocalDateTime.of(2026, 9, 20, 9, 0);
    LocalDateTime morningEnd = LocalDateTime.of(2026, 9, 20, 12, 0);
    LocalDateTime afternoonStart = LocalDateTime.of(2026, 9, 20, 12, 0);
    LocalDateTime afternoonEnd = LocalDateTime.of(2026, 9, 20, 15, 0);

    assertThat(CompetitionRefereeGroupScheduleGuard.overlaps(
        morningStart, morningEnd, afternoonStart, afternoonEnd)).isFalse();
  }

  @Test
  void missingStageTimeDoesNotProveOverlap() {
    assertThat(CompetitionRefereeGroupScheduleGuard.overlaps(
        LocalDateTime.of(2026, 9, 20, 9, 0),
        LocalDateTime.of(2026, 9, 20, 12, 0),
        null,
        LocalDateTime.of(2026, 9, 20, 13, 0))).isFalse();
  }
}
