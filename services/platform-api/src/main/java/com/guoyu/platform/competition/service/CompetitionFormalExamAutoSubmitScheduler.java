package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.exam.observability.FormalExamMetrics;
import com.guoyu.platform.competition.exam.observability.FormalExamNodeIds;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@EnableScheduling
@ConditionalOnProperty(
    prefix = "platform.competition.formal-exam.auto-submit",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class CompetitionFormalExamAutoSubmitScheduler {

  private final CompetitionFormalExamSessionMapper sessionMapper;
  private final CompetitionFormalExamExpiredSessionFinalizer finalizer;
  private final Clock clock;
  private final ThreadPoolTaskExecutor finalizeExecutor;
  private final FormalExamMetrics metrics;
  private final String nodeId;
  private final int claimSeconds;

  public CompetitionFormalExamAutoSubmitScheduler(
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalExamExpiredSessionFinalizer finalizer,
      Clock clock) {
    this(sessionMapper, finalizer, clock, null, null, "", 25);
  }

  @Autowired
  public CompetitionFormalExamAutoSubmitScheduler(
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalExamExpiredSessionFinalizer finalizer,
      Clock clock,
      @Qualifier("formalExamFinalizeExecutor") ThreadPoolTaskExecutor finalizeExecutor,
      ObjectProvider<FormalExamMetrics> metrics,
      @Value("${platform.competition.formal-exam.node-id:}") String nodeId,
      @Value("${platform.competition.formal-exam.auto-submit.claim-seconds:25}") int claimSeconds) {
    this.sessionMapper = sessionMapper;
    this.finalizer = finalizer;
    this.clock = clock;
    this.finalizeExecutor = finalizeExecutor;
    this.metrics = metrics == null ? null : metrics.getIfAvailable();
    this.nodeId = FormalExamNodeIds.current(nodeId);
    this.claimSeconds = Math.max(5, claimSeconds);
  }

  @Scheduled(fixedDelayString = "${platform.competition.formal-exam.auto-submit.fixed-delay-millis:5000}")
  public void scanDueSessions() {
    LocalDateTime now = LocalDateTime.now(clock);
    List<Long> tenantIds = sessionMapper.selectExpiredRunningTenantIds(now);
    if (tenantIds == null || tenantIds.isEmpty()) {
      return;
    }
    for (Long tenantId : tenantIds) {
      if (tenantId == null) {
        continue;
      }
      TenantContextHolder.setTenantId(tenantId);
      try {
        List<CompetitionFormalExamSessionEntity> sessions =
            sessionMapper.selectExpiredRunningSessions(now);
        if (sessions == null) {
          continue;
        }
        for (CompetitionFormalExamSessionEntity session : sessions) {
          if (session == null || session.getId() == null) {
            continue;
          }
          try {
            int claimed = sessionMapper.claimExpiredSession(
                session.getId(), nodeId, now.plusSeconds(claimSeconds), now);
            if (metrics != null) {
              metrics.recordClaim(claimed == 1);
            }
            if (claimed != 1) {
              continue;
            }
            submitFinalize(tenantId, session, now);
          } catch (RuntimeException ex) {
            log.error("Formal exam expired session finalization failed, tenantId={}, sessionId={}",
                tenantId, session.getId(), ex);
          }
        }
      } catch (RuntimeException ex) {
        log.error("Formal exam expired session scan failed, tenantId={}", tenantId, ex);
      } finally {
        TenantContextHolder.clear();
      }
    }
  }

  private void submitFinalize(
      Long tenantId, CompetitionFormalExamSessionEntity session, LocalDateTime now) {
    if (finalizeExecutor == null) {
      finalizer.finalizeExpiredSession(session, now);
      return;
    }
    try {
      finalizeExecutor.execute(() -> {
        TenantContextHolder.setTenantId(tenantId);
        try {
          finalizer.finalizeExpiredSession(session, now);
        } catch (RuntimeException ex) {
          log.error("Formal exam expired session finalization failed, tenantId={}, sessionId={}",
              tenantId, session.getId(), ex);
        } finally {
          TenantContextHolder.clear();
        }
      });
    } catch (RejectedExecutionException ex) {
      log.warn("Formal exam finalize executor busy, sessionId={}", session.getId());
    }
  }
}
