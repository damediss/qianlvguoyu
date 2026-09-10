package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionFormalExamAutoSubmitSchedulerTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
  private static final Instant NOW = Instant.parse("2026-09-08T02:30:04Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZONE);
  private static final LocalDateTime SERVER_NOW = LocalDateTime.ofInstant(NOW, ZONE);

  @AfterEach
  void clearTenant() {
    TenantContextHolder.clear();
  }

  @Test
  void workerSetsTenantPerScanAndAlwaysClearsContext() {
    CompetitionFormalExamSessionMapper sessions = Mockito.mock(CompetitionFormalExamSessionMapper.class);
    CompetitionFormalExamExpiredSessionFinalizer finalizer =
        Mockito.mock(CompetitionFormalExamExpiredSessionFinalizer.class);
    CompetitionFormalExamSessionEntity sessionA = expiredSession(9001L);
    CompetitionFormalExamSessionEntity sessionB = expiredSession(9002L);
    List<Long> seenTenants = new ArrayList<>();
    when(sessions.selectExpiredRunningTenantIds(SERVER_NOW)).thenReturn(List.of(1L, 2L));
    when(sessions.claimExpiredSession(any(), any(), any(), any())).thenReturn(1);
    when(sessions.selectExpiredRunningSessions(SERVER_NOW)).thenAnswer(invocation -> {
      seenTenants.add(TenantContextHolder.getTenantId());
      return Long.valueOf(1L).equals(TenantContextHolder.getTenantId())
          ? List.of(sessionA) : List.of(sessionB);
    });

    new CompetitionFormalExamAutoSubmitScheduler(sessions, finalizer, CLOCK).scanDueSessions();

    assertThat(seenTenants).containsExactly(1L, 2L);
    verify(finalizer).finalizeExpiredSession(sessionA, SERVER_NOW);
    verify(finalizer).finalizeExpiredSession(sessionB, SERVER_NOW);
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }

  @Test
  void clearsTenantContextWhenScanThrows() {
    CompetitionFormalExamSessionMapper sessions = Mockito.mock(CompetitionFormalExamSessionMapper.class);
    CompetitionFormalExamExpiredSessionFinalizer finalizer =
        Mockito.mock(CompetitionFormalExamExpiredSessionFinalizer.class);
    when(sessions.selectExpiredRunningTenantIds(SERVER_NOW)).thenReturn(List.of(1L));
    when(sessions.selectExpiredRunningSessions(SERVER_NOW)).thenThrow(new IllegalStateException("boom"));

    new CompetitionFormalExamAutoSubmitScheduler(sessions, finalizer, CLOCK).scanDueSessions();

    assertThat(TenantContextHolder.getTenantId()).isNull();
    verify(finalizer, never()).finalizeExpiredSession(any(), any());
  }

  @Test
  void sessionFailureDoesNotBlockSiblingSession() {
    CompetitionFormalExamSessionMapper sessions = Mockito.mock(CompetitionFormalExamSessionMapper.class);
    CompetitionFormalExamExpiredSessionFinalizer finalizer =
        Mockito.mock(CompetitionFormalExamExpiredSessionFinalizer.class);
    CompetitionFormalExamSessionEntity sessionA = expiredSession(9001L);
    CompetitionFormalExamSessionEntity sessionB = expiredSession(9002L);
    when(sessions.selectExpiredRunningTenantIds(SERVER_NOW)).thenReturn(List.of(1L));
    when(sessions.claimExpiredSession(any(), any(), any(), any())).thenReturn(1);
    when(sessions.selectExpiredRunningSessions(SERVER_NOW)).thenReturn(List.of(sessionA, sessionB));
    Mockito.doThrow(new IllegalStateException("boom")).when(finalizer)
        .finalizeExpiredSession(sessionA, SERVER_NOW);

    new CompetitionFormalExamAutoSubmitScheduler(sessions, finalizer, CLOCK).scanDueSessions();

    verify(finalizer).finalizeExpiredSession(sessionB, SERVER_NOW);
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }

  @Test
  void doesNotFinalizeBeforeDeadline() {
    CompetitionFormalExamSessionMapper sessions = Mockito.mock(CompetitionFormalExamSessionMapper.class);
    CompetitionFormalExamExpiredSessionFinalizer finalizer =
        Mockito.mock(CompetitionFormalExamExpiredSessionFinalizer.class);
    when(sessions.selectExpiredRunningTenantIds(SERVER_NOW)).thenReturn(List.of());

    new CompetitionFormalExamAutoSubmitScheduler(sessions, finalizer, CLOCK).scanDueSessions();

    verify(finalizer, never()).finalizeExpiredSession(any(), any());
  }

  @Test
  void skipsSessionsThatLoseTheExpireClaim() {
    CompetitionFormalExamSessionMapper sessions = Mockito.mock(CompetitionFormalExamSessionMapper.class);
    CompetitionFormalExamExpiredSessionFinalizer finalizer =
        Mockito.mock(CompetitionFormalExamExpiredSessionFinalizer.class);
    when(sessions.selectExpiredRunningTenantIds(SERVER_NOW)).thenReturn(List.of(1L));
    when(sessions.selectExpiredRunningSessions(SERVER_NOW)).thenReturn(List.of(expiredSession(9001L)));
    when(sessions.claimExpiredSession(any(), any(), any(), any())).thenReturn(0);

    new CompetitionFormalExamAutoSubmitScheduler(sessions, finalizer, CLOCK).scanDueSessions();

    verify(finalizer, never()).finalizeExpiredSession(any(), any());
  }

  private static CompetitionFormalExamSessionEntity expiredSession(Long id) {
    CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
    session.setId(id);
    session.setTenantId(1L);
    session.setStatus("running");
    session.setActualStartTime(SERVER_NOW.minusMinutes(90));
    session.setDurationSeconds(5400);
    return session;
  }
}
