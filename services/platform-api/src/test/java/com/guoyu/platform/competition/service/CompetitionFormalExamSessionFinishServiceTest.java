package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalStatusCountRow;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionFormalExamSessionFinishServiceTest {

  private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 9, 8, 10, 30, 0);
  private static final LocalDateTime SCAN = LocalDateTime.of(2026, 9, 8, 10, 30, 8);

  private final CompetitionFormalExamSessionMapper sessions =
      Mockito.mock(CompetitionFormalExamSessionMapper.class);
  private final CompetitionFormalCandidateExamMapper candidates =
      Mockito.mock(CompetitionFormalCandidateExamMapper.class);
  private final CompetitionFormalExamSessionFinishService service =
      new CompetitionFormalExamSessionFinishService(sessions, candidates);

  @Test
  void finishesWithDeadlineAsActualEndTimeAndReleasesOccupancy() {
    CompetitionFormalExamSessionEntity running = runningSession();
    when(sessions.selectByIdForUpdate(9001L)).thenReturn(running);
    when(candidates.countStatusByExamSessionId(9001L)).thenReturn(statusCounts(
        "submitted", "auto_submitted", "forced_submitted", "absent"));
    when(sessions.markFinishedIfRunning(
        eq(9001L), eq("running"), eq("finished"), eq(DEADLINE), eq(9001L), eq(SCAN)))
        .thenReturn(1);

    assertThat(service.finishIfEligible(9001L, DEADLINE, SCAN)).isTrue();
    verify(sessions).markFinishedIfRunning(
        eq(9001L), eq("running"), eq("finished"), eq(DEADLINE), eq(9001L), eq(SCAN));
  }

  @Test
  void doesNotFinishBeforeDeadline() {
    when(sessions.selectByIdForUpdate(9001L)).thenReturn(runningSession());
    when(candidates.countStatusByExamSessionId(9001L)).thenReturn(statusCounts(
        "submitted", "submitted"));

    assertThat(service.finishIfEligible(
        9001L, DEADLINE, LocalDateTime.of(2026, 9, 8, 10, 29, 0))).isFalse();
    verify(sessions, never()).markFinishedIfRunning(any(), any(), any(), any(), any(), any());
  }

  @Test
  void readyCandidateBlocksFinish() {
    when(sessions.selectByIdForUpdate(9001L)).thenReturn(runningSession());
    when(candidates.countStatusByExamSessionId(9001L)).thenReturn(statusCounts(
        "submitted", "ready", "submitted", "submitted"));

    assertThat(service.finishIfEligible(9001L, DEADLINE, SCAN)).isFalse();
    verify(sessions, never()).markFinishedIfRunning(any(), any(), any(), any(), any(), any());
  }

  @Test
  void candidateTotalMismatchBlocksFinish() {
    CompetitionFormalExamSessionEntity running = runningSession();
    running.setCandidateTotal(4);
    when(sessions.selectByIdForUpdate(9001L)).thenReturn(running);
    when(candidates.countStatusByExamSessionId(9001L)).thenReturn(statusCounts(
        "submitted", "absent"));

    assertThat(service.finishIfEligible(9001L, DEADLINE, SCAN)).isFalse();
    verify(sessions, never()).markFinishedIfRunning(any(), any(), any(), any(), any(), any());
  }

  @Test
  void alreadyFinishedIsIdempotent() {
    CompetitionFormalExamSessionEntity finished = runningSession();
    finished.setStatus("finished");
    finished.setActualEndTime(DEADLINE);
    finished.setOccupancyToken(9001L);
    when(sessions.selectByIdForUpdate(9001L)).thenReturn(finished);

    assertThat(service.finishIfEligible(9001L, DEADLINE, SCAN.plusSeconds(10))).isFalse();
    verify(sessions, never()).markFinishedIfRunning(any(), any(), any(), any(), any(), any());
    assertThat(finished.getActualEndTime()).isEqualTo(DEADLINE);
    assertThat(finished.getOccupancyToken()).isEqualTo(9001L);
  }

  @Test
  void concurrentFinishConvertsRunningOnlyOnce() throws Exception {
    AtomicReference<String> status = new AtomicReference<>("running");
    AtomicInteger conversions = new AtomicInteger();
    Object lock = new Object();
    when(sessions.selectByIdForUpdate(9001L)).thenAnswer(invocation -> {
      synchronized (lock) {
        CompetitionFormalExamSessionEntity session = runningSession();
        session.setStatus(status.get());
        if ("finished".equals(status.get())) {
          session.setActualEndTime(DEADLINE);
          session.setOccupancyToken(9001L);
        }
        return session;
      }
    });
    when(candidates.countStatusByExamSessionId(9001L)).thenReturn(statusCounts(
        "submitted",
        "auto_submitted",
        "forced_submitted",
        "absent"));
    when(sessions.markFinishedIfRunning(
        eq(9001L), eq("running"), eq("finished"), eq(DEADLINE), eq(9001L), eq(SCAN)))
        .thenAnswer(invocation -> {
          synchronized (lock) {
            if (!"running".equals(status.get())) {
              return 0;
            }
            status.set("finished");
            conversions.incrementAndGet();
            return 1;
          }
        });
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch go = new CountDownLatch(1);
    try {
      Future<Boolean> first = pool.submit(() -> {
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        return service.finishIfEligible(9001L, DEADLINE, SCAN);
      });
      Future<Boolean> second = pool.submit(() -> {
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        return service.finishIfEligible(9001L, DEADLINE, SCAN);
      });
      ready.await(5, TimeUnit.SECONDS);
      go.countDown();
      assertThat(List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(true, false);
      assertThat(conversions.get()).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  private static List<CompetitionFormalStatusCountRow> statusCounts(String... statuses) {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (String status : statuses) {
      counts.merge(status, 1L, Long::sum);
    }
    List<CompetitionFormalStatusCountRow> rows = new ArrayList<>();
    counts.forEach((status, total) -> {
      CompetitionFormalStatusCountRow row = new CompetitionFormalStatusCountRow();
      row.setStatus(status);
      row.setTotal(total);
      rows.add(row);
    });
    return rows;
  }

  private static CompetitionFormalExamSessionEntity runningSession() {
    CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
    session.setId(9001L);
    session.setStatus("running");
    session.setCandidateTotal(4);
    session.setActualStartTime(LocalDateTime.of(2026, 9, 8, 9, 0, 0));
    session.setDurationSeconds(5400);
    session.setOccupancyToken(0L);
    return session;
  }
}
