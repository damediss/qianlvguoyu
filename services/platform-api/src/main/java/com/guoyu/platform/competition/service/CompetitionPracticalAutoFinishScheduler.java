package com.guoyu.platform.competition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Records server-side AUTO_FINISH cutoffs from the immutable published plan timing snapshot. */
@Component
@Slf4j
public class CompetitionPracticalAutoFinishScheduler {
  private static final int CHUNK_SIZE = 100;
  private static final int MAX_CHUNKS_PER_TICK = 5;

  private final CompetitionPracticalAssignmentMapper assignmentMapper;

  public CompetitionPracticalAutoFinishScheduler(
      CompetitionPracticalAssignmentMapper assignmentMapper) {
    this.assignmentMapper = assignmentMapper;
  }

  @Scheduled(fixedDelayString = "${platform.competition.practical.auto-finish.fixed-delay-millis:5000}")
  public void finishExpiredAssignments() {
    LocalDateTime now = LocalDateTime.now();
    List<Long> tenantIds = assignmentMapper.selectTenantIdsForExpiredPracticalRuntime(now);
    if (tenantIds == null || tenantIds.isEmpty()) return;
    for (Long tenantId : tenantIds) {
      if (tenantId == null) continue;
      TenantContextHolder.setTenantId(tenantId);
      try {
        finishTenantAssignments(now);
      } catch (RuntimeException ex) {
        log.error("Practical AUTO_FINISH scan failed, tenantId={}", tenantId, ex);
      } finally {
        TenantContextHolder.clear();
      }
    }
  }

  private void finishTenantAssignments(LocalDateTime now) {
    for (int chunk = 0; chunk < MAX_CHUNKS_PER_TICK; chunk++) {
      List<CompetitionPracticalAssignmentEntity> due = assignmentMapper.selectList(
          new LambdaQueryWrapper<CompetitionPracticalAssignmentEntity>()
              .isNotNull(CompetitionPracticalAssignmentEntity::getDeadlineAt)
              .le(CompetitionPracticalAssignmentEntity::getDeadlineAt, now)
              .isNull(CompetitionPracticalAssignmentEntity::getExamEndedAt)
              .eq(CompetitionPracticalAssignmentEntity::getStatus, "in_progress")
              .eq(CompetitionPracticalAssignmentEntity::getDeleted, 0)
              .orderByAsc(CompetitionPracticalAssignmentEntity::getDeadlineAt)
              .last("LIMIT " + CHUNK_SIZE));
      if (due == null || due.isEmpty()) return;
      int ended = 0;
      for (CompetitionPracticalAssignmentEntity assignment : due) {
        if (assignment == null || assignment.getId() == null || assignment.getDeadlineAt() == null) {
          continue;
        }
        int updated = assignmentMapper.update(null,
            new LambdaUpdateWrapper<CompetitionPracticalAssignmentEntity>()
                .eq(CompetitionPracticalAssignmentEntity::getId, assignment.getId())
                .eq(CompetitionPracticalAssignmentEntity::getStatus, "in_progress")
                .isNull(CompetitionPracticalAssignmentEntity::getExamEndedAt)
                .le(CompetitionPracticalAssignmentEntity::getDeadlineAt, now)
                .set(CompetitionPracticalAssignmentEntity::getExamEndedAt,
                    assignment.getDeadlineAt())
                .set(CompetitionPracticalAssignmentEntity::getUpdatedAt, now));
        ended += updated;
      }
      if (ended > 0) {
        log.info("Practical assignments auto-finished at configured deadline, count={}", ended);
      }
      if (due.size() < CHUNK_SIZE) return;
    }
  }
}
