package com.guoyu.platform.competition.service;

import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionFormalScoringTaskEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalScoringTaskMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "platform.competition.formal-exam.scoring-messaging",
    name = "enabled", havingValue = "true", matchIfMissing = true)
public class CompetitionFormalScoringRecoveryScheduler {
  private final CompetitionFormalScoringTaskMapper taskMapper;
  private final CompetitionFormalCandidateExamMapper candidateMapper;
  private final CompetitionFormalScoringTaskService taskService;

  public CompetitionFormalScoringRecoveryScheduler(CompetitionFormalScoringTaskMapper taskMapper,
      CompetitionFormalCandidateExamMapper candidateMapper,
      CompetitionFormalScoringTaskService taskService) {
    this.taskMapper = taskMapper;
    this.candidateMapper = candidateMapper;
    this.taskService = taskService;
  }

  @Scheduled(fixedDelayString =
      "${platform.competition.formal-exam.scoring-messaging.recovery-delay-millis:5000}")
  public void recover() {
    LocalDateTime now = LocalDateTime.now();
    List<Long> tenants = new java.util.ArrayList<>();
    List<Long> recoverableTenants = taskMapper.selectRecoverableTenantIds(now);
    if (recoverableTenants != null) tenants.addAll(recoverableTenants);
    List<Long> missingTaskTenants = candidateMapper.selectMissingInitialScoringTenantIds();
    if (missingTaskTenants != null) {
      for (Long tenantId : missingTaskTenants) {
        if (!tenants.contains(tenantId)) tenants.add(tenantId);
      }
    }
    for (Long tenantId : tenants) {
      if (tenantId == null) continue;
      TenantContextHolder.setTenantId(tenantId);
      try {
        List<CompetitionFormalCandidateExamEntity> missingCandidates =
            candidateMapper.selectMissingInitialScoringCandidates(100);
        if (missingCandidates != null) {
          for (CompetitionFormalCandidateExamEntity candidate : missingCandidates) {
            try {
              taskService.enqueueInitial(candidate, "missing-task-recovery", null);
            } catch (RuntimeException ex) {
              log.error("Missing formal scoring task recovery failed, tenantId={}, candidateExamId={}",
                  tenantId, candidate == null ? null : candidate.getId(), ex);
            }
          }
        }
        List<CompetitionFormalScoringTaskEntity> tasks = taskMapper.selectRecoverable(now, 100);
        if (tasks != null) {
          for (CompetitionFormalScoringTaskEntity task : tasks) {
            try {
              taskService.recoverAndRepublish(task);
            } catch (RuntimeException ex) {
              log.error("Formal scoring task recovery failed, tenantId={}, taskId={}",
                  tenantId, task == null ? null : task.getId(), ex);
            }
          }
        }
      } finally {
        TenantContextHolder.clear();
      }
    }
  }
}
