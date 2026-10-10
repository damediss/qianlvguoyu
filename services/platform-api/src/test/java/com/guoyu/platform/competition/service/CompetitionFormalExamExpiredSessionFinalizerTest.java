package com.guoyu.platform.competition.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.exam.formal.FormalExamErrorCodes;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

class CompetitionFormalExamExpiredSessionFinalizerTest {

  private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 9, 8, 10, 30, 0);
  private static final LocalDateTime SCAN = LocalDateTime.of(2026, 9, 8, 10, 30, 4);

  private final CompetitionFormalCandidateExamMapper candidates =
      Mockito.mock(CompetitionFormalCandidateExamMapper.class);
  private final CompetitionFormalExamCandidateFinalizationService finalization =
      Mockito.mock(CompetitionFormalExamCandidateFinalizationService.class);
  private final CompetitionFormalExamSessionFinishService finish =
      Mockito.mock(CompetitionFormalExamSessionFinishService.class);
  private final CompetitionFormalExamScoringService scoring =
      Mockito.mock(CompetitionFormalExamScoringService.class);
  private final CompetitionFormalExamExpiredSessionFinalizer finalizer =
      new CompetitionFormalExamExpiredSessionFinalizer(candidates, finalization, finish, scoring);

  @Test
  void marksAbsentThenAutoSubmitsThenFinishesWithoutScoring() {
    CompetitionFormalExamSessionEntity session = expiredSession();
    when(candidates.markAbsentIfWaitingEnterBySession(
        eq(9001L), eq("waiting_enter"), eq("absent"), eq(DEADLINE),
        eq(FormalExamErrorCodes.ABSENT_REASON_DEADLINE_NOT_ENTERED), eq(SCAN)))
        .thenReturn(4);
    when(finalization.autoSubmitRunningChunk(9001L, DEADLINE, SCAN, 100)).thenReturn(3);
    when(candidates.countBySessionAndStatus(9001L, "ready")).thenReturn(0);

    finalizer.finalizeExpiredSession(session, SCAN);

    InOrder order = inOrder(candidates, finalization, finish);
    order.verify(candidates).markAbsentIfWaitingEnterBySession(
        eq(9001L), eq("waiting_enter"), eq("absent"), eq(DEADLINE),
        eq(FormalExamErrorCodes.ABSENT_REASON_DEADLINE_NOT_ENTERED), eq(SCAN));
    order.verify(finalization).autoSubmitRunningChunk(9001L, DEADLINE, SCAN, 100);
    order.verify(finish).finishIfEligible(9001L, DEADLINE, SCAN);
    verify(scoring, never()).scoreFinishedSession(any());
    verify(finalization, never()).autoSubmitIfRunning(any(), any(), any());
    verify(finalization, never()).markAbsentIfWaitingEnter(any(), any(), any());
  }

  @Test
  void doesNothingBeforeDeadline() {
    CompetitionFormalExamSessionEntity session = expiredSession();
    session.setActualStartTime(SCAN.minusMinutes(10));

    finalizer.finalizeExpiredSession(session, SCAN);

    verify(finalization, never()).autoSubmitRunningChunk(any(), any(), any(), anyInt());
    verify(candidates, never()).markAbsentIfWaitingEnterBySession(
        any(), any(), any(), any(), any(), any());
    verify(finish, never()).finishIfEligible(any(), any(), any());
    verify(scoring, never()).scoreFinishedSession(any());
  }

  @Test
  void readyIsCountedNotConvertedAndFinishIsStillAttempted() {
    CompetitionFormalExamSessionEntity session = expiredSession();
    when(candidates.markAbsentIfWaitingEnterBySession(
        any(), any(), any(), any(), any(), any())).thenReturn(0);
    when(finalization.autoSubmitRunningChunk(9001L, DEADLINE, SCAN, 100)).thenReturn(0);
    when(candidates.countBySessionAndStatus(9001L, "ready")).thenReturn(1);

    finalizer.finalizeExpiredSession(session, SCAN);

    verify(finalization, never()).autoSubmitIfRunning(eq(9103L), any(), any());
    verify(finish).finishIfEligible(9001L, DEADLINE, SCAN);
    verify(scoring, never()).scoreFinishedSession(any());
  }

  @Test
  void autoSubmitLoopsChunksUntilPartialThenStops() {
    CompetitionFormalExamExpiredSessionFinalizer chunked =
        new CompetitionFormalExamExpiredSessionFinalizer(
            candidates, finalization, finish, 2, 5, null);
    CompetitionFormalExamSessionEntity session = expiredSession();
    when(candidates.markAbsentIfWaitingEnterBySession(
        any(), any(), any(), any(), any(), any())).thenReturn(0);
    when(finalization.autoSubmitRunningChunk(9001L, DEADLINE, SCAN, 2)).thenReturn(2, 2, 1);
    when(candidates.countBySessionAndStatus(9001L, "ready")).thenReturn(0);

    chunked.finalizeExpiredSession(session, SCAN);

    verify(finalization, times(3)).autoSubmitRunningChunk(9001L, DEADLINE, SCAN, 2);
    verify(finish).finishIfEligible(9001L, DEADLINE, SCAN);
  }

  private static CompetitionFormalExamSessionEntity expiredSession() {
    CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
    session.setId(9001L);
    session.setStatus("running");
    session.setActualStartTime(LocalDateTime.of(2026, 9, 8, 9, 0, 0));
    session.setDurationSeconds(5400);
    session.setOccupancyToken(0L);
    return session;
  }
}
