package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionStageUnlockOutboxEntity;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageUnlockOutboxMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Applies promotion events outside the request thread with an explicit tenant boundary. */
@Component
@Slf4j
public class CompetitionStageUnlockOutboxProcessor {
  private final CompetitionStageUnlockOutboxMapper outboxMapper;
  private final CompetitionContestantProfileMapper profileMapper;

  public CompetitionStageUnlockOutboxProcessor(CompetitionStageUnlockOutboxMapper outboxMapper,
      CompetitionContestantProfileMapper profileMapper) {
    this.outboxMapper = outboxMapper; this.profileMapper = profileMapper;
  }

  @Scheduled(fixedDelayString = "${platform.competition.stage-unlock.scan-delay-millis:5000}")
  public void process() {
    List<Long> tenantIds = outboxMapper.selectPendingTenantIds();
    if (tenantIds == null) return;
    for (Long tenantId : tenantIds) {
      if (tenantId == null) continue;
      TenantContextHolder.setTenantId(tenantId);
      try {
        List<CompetitionStageUnlockOutboxEntity> events = outboxMapper.selectPending(tenantId);
        if (events == null) continue;
        for (CompetitionStageUnlockOutboxEntity event : events) {
          CompetitionContestantProfileEntity profile = profileMapper.selectForUpdateByTenantAndMember(tenantId, event.getContestantMemberId());
          if (profile != null && event.getTargetStageId() != null) {
            profile.setCurrentStageId(event.getTargetStageId()); profile.setPromotionStatus("promoted"); profileMapper.updateById(profile);
          }
          outboxMapper.markCompleted(tenantId, event.getId());
        }
      } catch (RuntimeException ex) {
        log.error("Competition stage unlock outbox processing failed, tenantId={}", tenantId, ex);
      } finally {
        TenantContextHolder.clear();
      }
    }
  }
}
