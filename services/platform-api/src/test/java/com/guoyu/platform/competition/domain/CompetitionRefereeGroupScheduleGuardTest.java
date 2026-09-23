package com.guoyu.platform.competition.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class CompetitionRefereeGroupScheduleGuardTest {

  @Test
  void differentEventsCannotShareAnActiveGroup() {
    assertThat(CompetitionRefereeGroupScheduleGuard.occupiesDifferentEvent(
        20L, Set.of(21L))).isTrue();
  }

  @Test
  void sameEventCanOccupyMultipleStages() {
    assertThat(CompetitionRefereeGroupScheduleGuard.occupiesDifferentEvent(
        20L, Set.of(20L))).isFalse();
  }

  @Test
  void unboundGroupCanJoinAnyEvent() {
    assertThat(CompetitionRefereeGroupScheduleGuard.occupiesDifferentEvent(
        20L, Set.of())).isFalse();
  }
}
