package com.guoyu.platform.competition.exam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CompetitionTheoryExamSchedulerTest {

  @AfterEach
  void clearTenant() {
    TenantContextHolder.clear();
  }

  @Test
  void workerScopeSetsAndAlwaysClearsTenantContext() {
    CompetitionTheoryExamExecutionService service = mock(CompetitionTheoryExamExecutionService.class);
    Clock clock = Clock.fixed(Instant.parse("2026-07-31T10:00:00Z"), ZoneOffset.UTC);
    LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    CompetitionTheoryExamSessionEntity first = new CompetitionTheoryExamSessionEntity();
    first.setId(11L);
    CompetitionTheoryExamSessionEntity second = new CompetitionTheoryExamSessionEntity();
    second.setId(22L);
    when(service.executionTenantIds(now)).thenReturn(List.of(1L, 2L));
    when(service.sessionsForExecution(now)).thenAnswer(invocation ->
        TenantContextHolder.getTenantId().equals(1L) ? List.of(first) : List.of(second));
    doAnswer(invocation -> {
      assertThat(TenantContextHolder.getTenantId()).isEqualTo(invocation.getArgument(0));
      return null;
    }).when(service).processSession(any(Long.class), any(Long.class), eq(now));

    new CompetitionTheoryExamScheduler(service, clock).recoverAndAdvance();

    verify(service).processSession(1L, 11L, now);
    verify(service).processSession(2L, 22L, now);
    assertThat(TenantContextHolder.getTenantId()).isNull();
  }
}
