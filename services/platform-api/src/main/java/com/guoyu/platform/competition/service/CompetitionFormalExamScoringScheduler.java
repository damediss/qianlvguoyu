package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "platform.competition.formal-exam.scoring",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class CompetitionFormalExamScoringScheduler {

  private final CompetitionFormalExamSessionMapper sessionMapper;
  private final CompetitionFormalExamScoringService scoringService;
  private final Clock clock;

  public CompetitionFormalExamScoringScheduler(
      CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalExamScoringService scoringService,
      Clock clock) {
    this.sessionMapper = sessionMapper;
    this.scoringService = scoringService;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${platform.competition.formal-exam.scoring.fixed-delay-millis:5000}")
  public void scanDueSessions() {
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime stuckBefore = scoringService.stuckBefore(now);
    List<Long> tenantIds = sessionMapper.selectScoringDueTenantIds(stuckBefore);
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
            sessionMapper.selectScoringDueSessions(stuckBefore);
        if (sessions == null) {
          continue;
        }
        for (CompetitionFormalExamSessionEntity session : sessions) {
          try {
            scoringService.scoreFinishedSession(session == null ? null : session.getId());
          } catch (RuntimeException ex) {
            log.error("Formal exam scoring scan failed, tenantId={}, sessionId={}",
                tenantId, session == null ? null : session.getId(), ex);
          }
        }
      } catch (RuntimeException ex) {
        log.error("Formal exam scoring scan failed, tenantId={}", tenantId, ex);
      } finally {
        TenantContextHolder.clear();
      }
    }
  }
}
