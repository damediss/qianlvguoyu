package com.guoyu.platform.competition.exam.service;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionEntity;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CompetitionTheoryExamScheduler {
  private final CompetitionTheoryExamExecutionService executionService;
  private final Clock clock;

  public CompetitionTheoryExamScheduler(
      CompetitionTheoryExamExecutionService executionService, Clock clock) {
    this.executionService = executionService;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${platform.competition.theory-exam.scan-delay-millis:5000}")
  public void recoverAndAdvance() {
    LocalDateTime now = LocalDateTime.now(clock);
    for (Long tenantId : executionService.executionTenantIds(now)) {
      if (tenantId == null) {
        continue;
      }
      TenantContextHolder.setTenantId(tenantId);
      try {
        for (CompetitionTheoryExamSessionEntity session :
            executionService.sessionsForExecution(now)) {
          executionService.processSession(tenantId, session.getId(), now);
        }
      } catch (RuntimeException ex) {
        log.error("Competition theory exam recovery failed, tenantId={}", tenantId, ex);
      } finally {
        TenantContextHolder.clear();
      }
    }
  }
}
