package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.exam.formal.FormalExamErrorCodes;
import com.guoyu.platform.competition.mapper.CompetitionFormalAnsweredCountRow;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionFormalExamCandidateFinalizationServiceTest {

  private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 9, 8, 10, 30, 0);
  private static final LocalDateTime SCAN_TIME = LocalDateTime.of(2026, 9, 8, 10, 30, 4);

  private final CompetitionFormalCandidateExamMapper candidateExams =
      Mockito.mock(CompetitionFormalCandidateExamMapper.class);
  private final CompetitionFormalCandidateAnswerMapper answers =
      Mockito.mock(CompetitionFormalCandidateAnswerMapper.class);
  private final CompetitionFormalExamCandidateFinalizationService service =
      new CompetitionFormalExamCandidateFinalizationService(candidateExams, answers);

  @Test
  void autoSubmitUsesDeadlineAsSubmitTimeAndDoesNotRewrite() {
    CompetitionFormalCandidateExamEntity running = candidate("running");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(running);
    when(answers.countAnswered(9101L)).thenReturn(8);
    when(candidateExams.markSubmittedIfRunning(
        eq(9101L), eq("running"), eq("auto_submitted"), eq(DEADLINE),
        eq(FormalExamErrorCodes.SUBMIT_TYPE_AUTO), eq(8), eq(SCAN_TIME), isNull())).thenReturn(1);
    CompetitionFormalCandidateExamEntity submitted = candidate("auto_submitted");
    submitted.setSubmitTime(DEADLINE);
    submitted.setSubmitType(FormalExamErrorCodes.SUBMIT_TYPE_AUTO);
    submitted.setAnsweredCount(8);
    when(candidateExams.selectById(9101L)).thenReturn(submitted);

    CompetitionFormalCandidateExamEntity first =
        service.autoSubmitIfRunning(9101L, DEADLINE, SCAN_TIME);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(submitted);
    CompetitionFormalCandidateExamEntity second =
        service.autoSubmitIfRunning(9101L, DEADLINE, SCAN_TIME.plusSeconds(5));

    assertThat(first.getStatus()).isEqualTo("auto_submitted");
    assertThat(first.getSubmitTime()).isEqualTo(DEADLINE);
    assertThat(second.getSubmitTime()).isEqualTo(DEADLINE);
    verify(candidateExams).markSubmittedIfRunning(
        eq(9101L), eq("running"), eq("auto_submitted"), eq(DEADLINE),
        eq(FormalExamErrorCodes.SUBMIT_TYPE_AUTO), eq(8), eq(SCAN_TIME), isNull());
  }

  @Test
  void submittedIsNotConvertedToAutoSubmitted() {
    CompetitionFormalCandidateExamEntity submitted = candidate("submitted");
    submitted.setSubmitTime(DEADLINE.minusMinutes(10));
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(submitted);

    CompetitionFormalCandidateExamEntity result =
        service.autoSubmitIfRunning(9101L, DEADLINE, SCAN_TIME);

    assertThat(result.getStatus()).isEqualTo("submitted");
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  void forcedIsNotConvertedToAutoSubmitted() {
    CompetitionFormalCandidateExamEntity forced = candidate("forced_submitted");
    forced.setSubmitTime(DEADLINE.minusMinutes(3));
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(forced);

    CompetitionFormalCandidateExamEntity result =
        service.autoSubmitIfRunning(9101L, DEADLINE, SCAN_TIME);

    assertThat(result.getStatus()).isEqualTo("forced_submitted");
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  void forceSubmitWritesReasonAndOperatorOnce() {
    CompetitionFormalCandidateExamEntity running = candidate("running");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(running);
    when(answers.countAnswered(9101L)).thenReturn(4);
    when(candidateExams.markForcedIfRunning(
        eq(9101L), eq("running"), eq("forced_submitted"), eq(SCAN_TIME),
        eq(FormalExamErrorCodes.SUBMIT_TYPE_FORCED), eq(4), eq("现场设备异常"), eq(1000L),
        eq(SCAN_TIME), eq(1000L))).thenReturn(1);
    CompetitionFormalCandidateExamEntity forced = candidate("forced_submitted");
    forced.setSubmitTime(SCAN_TIME);
    forced.setSubmitType(FormalExamErrorCodes.SUBMIT_TYPE_FORCED);
    forced.setForceSubmitReason("现场设备异常");
    forced.setForceSubmittedBy(1000L);
    when(candidateExams.selectById(9101L)).thenReturn(forced);

    CompetitionFormalCandidateExamEntity first =
        service.forceSubmitIfRunning(9101L, SCAN_TIME, 1000L, "现场设备异常");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(forced);
    CompetitionFormalCandidateExamEntity second =
        service.forceSubmitIfRunning(9101L, SCAN_TIME.plusSeconds(8), 2000L, "另一原因");

    assertThat(first.getSubmitTime()).isEqualTo(SCAN_TIME);
    assertThat(first.getForceSubmitReason()).isEqualTo("现场设备异常");
    assertThat(first.getForceSubmittedBy()).isEqualTo(1000L);
    assertThat(second.getForceSubmitReason()).isEqualTo("现场设备异常");
    assertThat(second.getForceSubmittedBy()).isEqualTo(1000L);
    verify(candidateExams).markForcedIfRunning(
        eq(9101L), eq("running"), eq("forced_submitted"), eq(SCAN_TIME),
        eq(FormalExamErrorCodes.SUBMIT_TYPE_FORCED), eq(4), eq("现场设备异常"), eq(1000L),
        eq(SCAN_TIME), eq(1000L));
  }

  @Test
  void waitingEnterBecomesAbsentWithoutSubmitFields() {
    CompetitionFormalCandidateExamEntity waiting = candidate("waiting_enter");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(waiting);
    when(candidateExams.markAbsentIfWaitingEnter(
        eq(9101L), eq("waiting_enter"), eq("absent"), eq(DEADLINE),
        eq(FormalExamErrorCodes.ABSENT_REASON_DEADLINE_NOT_ENTERED), eq(SCAN_TIME))).thenReturn(1);
    CompetitionFormalCandidateExamEntity absent = candidate("absent");
    absent.setAbsentTime(DEADLINE);
    absent.setAbsentReason(FormalExamErrorCodes.ABSENT_REASON_DEADLINE_NOT_ENTERED);
    when(candidateExams.selectById(9101L)).thenReturn(absent);

    CompetitionFormalCandidateExamEntity result =
        service.markAbsentIfWaitingEnter(9101L, DEADLINE, SCAN_TIME);

    assertThat(result.getStatus()).isEqualTo("absent");
    assertThat(result.getAbsentTime()).isEqualTo(DEADLINE);
    assertThat(result.getSubmitTime()).isNull();
    assertThat(result.getSubmitType()).isNull();
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  void readyAndRunningAreNotArchivedAsAbsent() {
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("ready"));
    assertThat(service.markAbsentIfWaitingEnter(9101L, DEADLINE, SCAN_TIME).getStatus())
        .isEqualTo("ready");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    assertThat(service.markAbsentIfWaitingEnter(9101L, DEADLINE, SCAN_TIME).getStatus())
        .isEqualTo("running");
    verify(candidateExams, never()).markAbsentIfWaitingEnter(any(), any(), any(), any(), any(), any());
  }

  @Test
  void absentArchiveIsIdempotent() {
    CompetitionFormalCandidateExamEntity absent = candidate("absent");
    absent.setAbsentTime(DEADLINE);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(absent);

    CompetitionFormalCandidateExamEntity result =
        service.markAbsentIfWaitingEnter(9101L, DEADLINE.plusMinutes(1), SCAN_TIME);

    assertThat(result.getAbsentTime()).isEqualTo(DEADLINE);
    verify(candidateExams, never()).markAbsentIfWaitingEnter(any(), any(), any(), any(), any(), any());
  }

  @Test
  void waitingEnterIsNotAutoSubmitted() {
    CompetitionFormalCandidateExamEntity waiting = candidate("waiting_enter");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(waiting);

    CompetitionFormalCandidateExamEntity result =
        service.autoSubmitIfRunning(9101L, DEADLINE, SCAN_TIME);

    assertThat(result.getStatus()).isEqualTo("waiting_enter");
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  void readyIsNotAutoSubmitted() {
    CompetitionFormalCandidateExamEntity ready = candidate("ready");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(ready);

    CompetitionFormalCandidateExamEntity result =
        service.autoSubmitIfRunning(9101L, DEADLINE, SCAN_TIME);

    assertThat(result.getStatus()).isEqualTo("ready");
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  void concurrentAutoSubmitConvertsRunningOnlyOnce() throws Exception {
    AtomicReference<String> status = new AtomicReference<>("running");
    AtomicInteger conversions = new AtomicInteger();
    Object lock = new Object();
    when(candidateExams.selectByIdForUpdate(9101L)).thenAnswer(invocation -> {
      synchronized (lock) {
        return candidate(status.get());
      }
    });
    when(answers.countAnswered(9101L)).thenReturn(3);
    when(candidateExams.markSubmittedIfRunning(
        eq(9101L), eq("running"), eq("auto_submitted"), eq(DEADLINE),
        eq(FormalExamErrorCodes.SUBMIT_TYPE_AUTO), eq(3), eq(SCAN_TIME), isNull()))
        .thenAnswer(invocation -> {
          synchronized (lock) {
            if (!"running".equals(status.get())) {
              return 0;
            }
            status.set("auto_submitted");
            conversions.incrementAndGet();
            return 1;
          }
        });
    when(candidateExams.selectById(9101L)).thenAnswer(invocation -> {
      synchronized (lock) {
        CompetitionFormalCandidateExamEntity entity = candidate(status.get());
        entity.setSubmitTime(DEADLINE);
        entity.setSubmitType(FormalExamErrorCodes.SUBMIT_TYPE_AUTO);
        return entity;
      }
    });
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch go = new CountDownLatch(1);
    try {
      Future<CompetitionFormalCandidateExamEntity> first = pool.submit(() -> {
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        return service.autoSubmitIfRunning(9101L, DEADLINE, SCAN_TIME);
      });
      Future<CompetitionFormalCandidateExamEntity> second = pool.submit(() -> {
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        return service.autoSubmitIfRunning(9101L, DEADLINE, SCAN_TIME);
      });
      ready.await(5, TimeUnit.SECONDS);
      go.countDown();
      assertThat(first.get(5, TimeUnit.SECONDS).getStatus()).isEqualTo("auto_submitted");
      assertThat(second.get(5, TimeUnit.SECONDS).getStatus()).isEqualTo("auto_submitted");
      assertThat(conversions.get()).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void autoSubmitRunningChunkUsesBatchAnswerCountsAndCas() {
    when(candidateExams.selectIdsBySessionAndStatusLimit(9001L, "running", 2))
        .thenReturn(List.of(9101L, 9102L));
    CompetitionFormalAnsweredCountRow row = new CompetitionFormalAnsweredCountRow();
    row.setCandidateExamId(9101L);
    row.setTotal(8L);
    when(answers.countAnsweredByCandidateIds(List.of(9101L, 9102L))).thenReturn(List.of(row));
    when(candidateExams.markSubmittedIfRunning(
        eq(9101L), eq("running"), eq("auto_submitted"), eq(DEADLINE),
        eq(FormalExamErrorCodes.SUBMIT_TYPE_AUTO), eq(8), eq(SCAN_TIME), isNull())).thenReturn(1);
    when(candidateExams.markSubmittedIfRunning(
        eq(9102L), eq("running"), eq("auto_submitted"), eq(DEADLINE),
        eq(FormalExamErrorCodes.SUBMIT_TYPE_AUTO), eq(0), eq(SCAN_TIME), isNull())).thenReturn(1);

    assertThat(service.autoSubmitRunningChunk(9001L, DEADLINE, SCAN_TIME, 2)).isEqualTo(2);
    verify(answers, times(1)).countAnsweredByCandidateIds(List.of(9101L, 9102L));
    verify(answers, never()).countAnswered(any());
  }

  private CompetitionFormalCandidateExamEntity candidate(String status) {
    CompetitionFormalCandidateExamEntity entity = new CompetitionFormalCandidateExamEntity();
    entity.setId(9101L);
    entity.setExamSessionId(9001L);
    entity.setStatus(status);
    entity.setAnsweredCount(0);
    entity.setTotalQuestionCount(10);
    return entity;
  }
}
