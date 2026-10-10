package com.guoyu.platform.competition.exam.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class FormalExamConnectionStatesTest {

  private static final Instant NOW = Instant.parse("2026-09-10T01:00:00Z");

  @Test
  void businessTerminalStatesWinOverPresence() {
    assertThat(FormalExamConnectionStates.resolve(
        "absent", "active", NOW, NOW, 60)).isEqualTo(FormalExamConnectionStates.ABSENT);
    assertThat(FormalExamConnectionStates.resolve(
        "submitted", "kicked", NOW, NOW, 60)).isEqualTo(FormalExamConnectionStates.SUBMITTED);
    assertThat(FormalExamConnectionStates.resolve(
        "auto_submitted", "active", NOW, NOW, 60)).isEqualTo(FormalExamConnectionStates.SUBMITTED);
    assertThat(FormalExamConnectionStates.resolve(
        "forced_submitted", "expired", NOW, NOW, 60)).isEqualTo(FormalExamConnectionStates.SUBMITTED);
  }

  @Test
  void accessSessionKickAndExpiryBeatLastSeen() {
    assertThat(FormalExamConnectionStates.resolve(
        "running", "kicked", NOW, NOW, 60)).isEqualTo(FormalExamConnectionStates.KICKED);
    assertThat(FormalExamConnectionStates.resolve(
        "running", "expired", NOW, NOW, 60)).isEqualTo(FormalExamConnectionStates.TOKEN_EXPIRED);
  }

  @Test
  void lastSeenWithinWindowIsOnlineOtherwiseBrieflyOffline() {
    assertThat(FormalExamConnectionStates.resolve(
        "running", "active", NOW.minusSeconds(30), NOW, 60))
        .isEqualTo(FormalExamConnectionStates.ONLINE);
    assertThat(FormalExamConnectionStates.resolve(
        "running", "active", NOW.minusSeconds(120), NOW, 60))
        .isEqualTo(FormalExamConnectionStates.BRIEFLY_OFFLINE);
    assertThat(FormalExamConnectionStates.resolve(
        "ready", "active", null, NOW, 60))
        .isEqualTo(FormalExamConnectionStates.BRIEFLY_OFFLINE);
  }
}
