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

class CompetitionFormalExamScoringSchedulerTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
  private static final Instant NOW = Instant.parse("2026-09-08T03:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZONE);
  private static final LocalDateTime SERVER_NOW = LocalDateTime.ofInstant(NOW, ZONE);
  private static final LocalDateTime STUCK_BEFORE = SERVER_NOW.minusMinutes(15);

  @AfterEach
  void clearTenant() {
    TenantContextHolder.clear();
  }

  @Test
  void workerSetsTenantPerScanAndAlwaysClearsContext() {
    CompetitionFormalExamSessionMapper sessions = Mockito.mock(CompetitionFormalExamSessionMapper.class);
    CompetitionFormalExamScoringService scoring = Mockito.mock(CompetitionFormalExamScoringService.class);
    when(scoring.stuckBefore(SERVER_NOW)).thenReturn(STUCK_BEFORE);
    when(sessions.selectScoringDueTenantIds(STUCK_BEFORE)).thenReturn(List.of(1L, 2L));
    List<Long> seenTenants = new ArrayList<>();
    when(sessions.selectScoringDueSessions(STUCK_BEFORE)).thenAnswer(invocation -> {
      seenTenants.add(TenantContextHolder.getTenantId());
      CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
      session.setId(Long.valueOf(1L).equals(TenantContextHolder.getTenantId()) ? 9001L : 9002L);
      return List.of(session);
    });

    new CompetitionFormalExamScoringScheduler(sessions, scoring, CLOCK).scanDueSessions();

    assertThat(seenTenants).containsExactly(1L, 2L);
    verify(scoring).scoreFinishedSession(9001L);
    verify(scoring).scoreFinishedSession(9002L);
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }

  @Test
  void clearsTenantWhenScanThrows() {
    CompetitionFormalExamSessionMapper sessions = Mockito.mock(CompetitionFormalExamSessionMapper.class);
    CompetitionFormalExamScoringService scoring = Mockito.mock(CompetitionFormalExamScoringService.class);
    when(scoring.stuckBefore(SERVER_NOW)).thenReturn(STUCK_BEFORE);
    when(sessions.selectScoringDueTenantIds(STUCK_BEFORE)).thenReturn(List.of(1L));
    when(sessions.selectScoringDueSessions(STUCK_BEFORE)).thenThrow(new IllegalStateException("boom"));

    new CompetitionFormalExamScoringScheduler(sessions, scoring, CLOCK).scanDueSessions();

    assertThat(TenantContextHolder.getTenantId()).isNull();
    verify(scoring, never()).scoreFinishedSession(any());
  }
}
