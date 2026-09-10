package com.guoyu.platform.competition.exam.formal;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.exam.formal.FormalExamSessionFinishRules.CandidateCounts;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class FormalExamSessionFinishRulesTest {

  private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 9, 8, 10, 30, 0);
  private static final LocalDateTime BEFORE = LocalDateTime.of(2026, 9, 8, 10, 29, 59);
  private static final LocalDateTime AFTER = LocalDateTime.of(2026, 9, 8, 10, 30, 8);

  @Test
  void doesNotFinishBeforeDeadlineEvenIfEveryoneSubmitted() {
    CompetitionFormalExamSessionEntity session = runningSession(4);
    CandidateCounts counts = FormalExamSessionFinishRules.counts(List.of(
        candidate("submitted"), candidate("submitted"),
        candidate("submitted"), candidate("submitted")));
    assertThat(FormalExamSessionFinishRules.finishEligible(session, counts, BEFORE)).isFalse();
    assertThat(FormalExamSessionFinishRules.blockers(session, counts, BEFORE)).isEmpty();
  }

  @Test
  void finishEligibleWhenAllClosedAfterDeadline() {
    CompetitionFormalExamSessionEntity session = runningSession(4);
    CandidateCounts counts = FormalExamSessionFinishRules.counts(List.of(
        candidate("submitted"),
        candidate("auto_submitted"),
        candidate("forced_submitted"),
        candidate("absent")));
    assertThat(counts.closed()).isEqualTo(4);
    assertThat(FormalExamSessionFinishRules.finishEligible(session, counts, AFTER)).isTrue();
    assertThat(FormalExamSessionFinishRules.blockers(session, counts, AFTER)).isEmpty();
  }

  @Test
  void readyBlocksFinishAndIsNotCountedAsClosed() {
    CompetitionFormalExamSessionEntity session = runningSession(2);
    CandidateCounts counts = FormalExamSessionFinishRules.counts(List.of(
        candidate("submitted"), candidate("ready")));
    assertThat(FormalExamSessionFinishRules.finishEligible(session, counts, AFTER)).isFalse();
    assertThat(FormalExamSessionFinishRules.blockers(session, counts, AFTER))
        .extracting(item -> item.code())
        .contains(FormalExamSessionFinishRules.READY_CANDIDATE_REMAINS);
  }

  @Test
  void waitingAndCountMismatchAreBlockers() {
    CompetitionFormalExamSessionEntity session = runningSession(4);
    CandidateCounts counts = FormalExamSessionFinishRules.counts(List.of(
        candidate("submitted"), candidate("waiting_enter")));
    assertThat(FormalExamSessionFinishRules.finishEligible(session, counts, AFTER)).isFalse();
    assertThat(FormalExamSessionFinishRules.blockers(session, counts, AFTER))
        .extracting(item -> item.code())
        .contains(
            FormalExamSessionFinishRules.UNRESOLVED_WAITING_CANDIDATE,
            FormalExamSessionFinishRules.CANDIDATE_TOTAL_MISMATCH);
  }

  @Test
  void submittedCountAloneIsNotTheFinishCondition() {
    CompetitionFormalExamSessionEntity session = runningSession(2);
    CandidateCounts counts = FormalExamSessionFinishRules.counts(List.of(
        candidate("submitted"), candidate("absent")));
    assertThat(counts.submitted()).isEqualTo(1);
    assertThat(counts.closed()).isEqualTo(2);
    assertThat(FormalExamSessionFinishRules.finishEligible(session, counts, AFTER)).isTrue();
  }

  @Test
  void deadlineTimeUsesActualStartPlusDuration() {
    CompetitionFormalExamSessionEntity session = runningSession(1);
    assertThat(FormalExamSessionFinishRules.deadlineTime(session)).isEqualTo(DEADLINE);
    assertThat(FormalExamSessionFinishRules.deadlineReached(session, AFTER)).isTrue();
    assertThat(FormalExamSessionFinishRules.deadlineReached(session, BEFORE)).isFalse();
  }

  private static CompetitionFormalExamSessionEntity runningSession(int candidateTotal) {
    CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
    session.setId(9001L);
    session.setStatus("running");
    session.setCandidateTotal(candidateTotal);
    session.setActualStartTime(LocalDateTime.of(2026, 9, 8, 9, 0, 0));
    session.setDurationSeconds(5400);
    session.setOccupancyToken(0L);
    return session;
  }

  private static CompetitionFormalCandidateExamEntity candidate(String status) {
    CompetitionFormalCandidateExamEntity entity = new CompetitionFormalCandidateExamEntity();
    entity.setStatus(status);
    return entity;
  }
}
