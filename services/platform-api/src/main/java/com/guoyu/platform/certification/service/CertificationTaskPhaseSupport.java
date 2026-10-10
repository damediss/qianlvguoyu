package com.guoyu.platform.certification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.entity.CertificationTaskPhaseEntity;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskPhaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 任务阶段（current_phase）的运行时支撑工具：阶段编码/名称的统一来源、阶段推进、
 * 历史数据的规范化。任务状态（status）的取值不再耦合到阶段，由调用方在推进时单独传入。
 */
public final class CertificationTaskPhaseSupport {

  public static final String TASK_CREATION = CertificationTaskPhases.TASK_CREATION;
  public static final String PREPARATION = CertificationTaskPhases.PREPARATION;
  public static final String ARCHIVE = CertificationTaskPhases.ARCHIVE;
  public static final String CERTIFICATION_COMPLETED = CertificationTaskPhases.CERTIFICATION_COMPLETED;

  /** 历史阶段编码：5 阶段流程留下的旧编码 + project_creation 旧名。 */
  private static final Set<String> LEGACY_PHASE_CODES = Set.of(
      "before_exam",
      "theory_exam",
      "practical_exam",
      "project_creation"
  );

  /** 任务创建阶段：五份资料尚未补齐归档时的阶段行状态。 */
  public static final String PHASE_STATUS_PENDING_COMPLETE = "pending_complete";

  static final List<PhaseSeed> PHASES = List.of(
      // 新建任务即进入认定筹备：任务创建阶段标记为「待补充完整」，筹备阶段为进行中
      new PhaseSeed(TASK_CREATION, "任务创建阶段", 1, PHASE_STATUS_PENDING_COMPLETE),
      new PhaseSeed(PREPARATION, "认定筹备阶段", 2, "running"),
      new PhaseSeed(ARCHIVE, "认定归档阶段", 3, "pending"),
      new PhaseSeed(CERTIFICATION_COMPLETED, "认定完成", 4, "pending")
  );

  private CertificationTaskPhaseSupport() {
  }

  public static String phaseName(String phase) {
    return CertificationTaskPhases.phaseName(phase);
  }

  /**
   * 推进任务阶段：标记 completedPhaseCode 为已完成、nextPhaseCode 为进行中，
   * 同时更新任务的 current_phase 与 status。status 由调用方决定（推进到普通阶段一般为 in_progress，
   * 推进到 CERTIFICATION_COMPLETED 一般为 completed）。
   */
  static void advancePhase(
      CertificationTaskMapper taskMapper,
      CertificationTaskPhaseMapper phaseMapper,
      Long taskId,
      String completedPhaseCode,
      String nextPhaseCode,
      String nextStatus
  ) {
    completePhase(phaseMapper, taskId, completedPhaseCode);
    if (!CERTIFICATION_COMPLETED.equals(nextPhaseCode)) {
      startPhase(phaseMapper, taskId, nextPhaseCode);
    } else {
      // 认定完成阶段直接置为 completed
      completePhase(phaseMapper, taskId, nextPhaseCode);
    }
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task != null) {
      task.setCurrentPhase(nextPhaseCode);
      task.setPhaseTotalCount(PHASES.size());
      task.setPhaseCompletedCount(countCompletedPhases(phaseMapper, taskId));
      task.setStatus(nextStatus);
      task.setLastBusinessUpdatedAt(LocalDateTime.now());
      taskMapper.updateById(task);
    }
  }

  static void completePhase(CertificationTaskPhaseMapper phaseMapper, Long taskId, String phaseCode) {
    CertificationTaskPhaseEntity phase = findPhase(phaseMapper, taskId, phaseCode);
    if (phase == null) {
      return;
    }
    phase.setStatus("completed");
    if (phase.getStartedAt() == null) {
      phase.setStartedAt(LocalDateTime.now());
    }
    if (phase.getCompletedAt() == null) {
      phase.setCompletedAt(LocalDateTime.now());
    }
    phaseMapper.updateById(phase);
  }

  static void startPhase(CertificationTaskPhaseMapper phaseMapper, Long taskId, String phaseCode) {
    CertificationTaskPhaseEntity phase = findPhase(phaseMapper, taskId, phaseCode);
    if (phase == null) {
      return;
    }
    phase.setStatus("running");
    if (phase.getStartedAt() == null) {
      phase.setStartedAt(LocalDateTime.now());
    }
    phaseMapper.updateById(phase);
  }

  static void syncPhaseProgress(
      CertificationTaskMapper taskMapper,
      CertificationTaskPhaseMapper phaseMapper,
      Long taskId
  ) {
    CertificationTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) {
      return;
    }
    task.setPhaseTotalCount(PHASES.size());
    task.setPhaseCompletedCount(countCompletedPhases(phaseMapper, taskId));
    taskMapper.updateById(task);
  }

  /**
   * 将历史阶段记录（含 project_creation 与 5 阶段遗留）规范化到当前 4 阶段 schema。
   * 这里只修复 certification_task_phase 中的编码与计数，状态推断由 SQL 迁移完成。
   */
  static void ensureCanonicalPhases(
      CertificationTaskMapper taskMapper,
      CertificationTaskPhaseMapper phaseMapper,
      CertificationTaskEntity task
  ) {
    if (task == null || task.getId() == null) {
      return;
    }
    Long taskId = task.getId();
    List<CertificationTaskPhaseEntity> phases = listPhases(phaseMapper, taskId);
    if (!needsPhaseRepair(phases)) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    // 1. project_creation -> task_creation：原地改名
    for (CertificationTaskPhaseEntity phase : phases) {
      if ("project_creation".equals(phase.getPhaseCode())) {
        if (findPhase(phaseMapper, taskId, TASK_CREATION) != null) {
          phaseMapper.deleteById(phase.getId());
        } else {
          phase.setPhaseCode(TASK_CREATION);
          phase.setPhaseName("任务创建阶段");
          phase.setPhaseOrder(1);
          phaseMapper.updateById(phase);
        }
      }
    }
    phases = listPhases(phaseMapper, taskId);

    // 2. 五阶段遗留：before_exam/theory_exam/practical_exam 软删除
    for (CertificationTaskPhaseEntity phase : phases) {
      if (LEGACY_PHASE_CODES.contains(phase.getPhaseCode())
          && !"project_creation".equals(phase.getPhaseCode())) {
        phaseMapper.deleteById(phase.getId());
      }
    }
    phases = listPhases(phaseMapper, taskId);

    // 3. preparation/archive 名称与序号矫正
    for (CertificationTaskPhaseEntity phase : phases) {
      if (PREPARATION.equals(phase.getPhaseCode())) {
        phase.setPhaseName("认定筹备阶段");
        phase.setPhaseOrder(2);
        phaseMapper.updateById(phase);
      }
      if (ARCHIVE.equals(phase.getPhaseCode())) {
        phase.setPhaseName("认定归档阶段");
        phase.setPhaseOrder(3);
        phaseMapper.updateById(phase);
      }
    }

    // 4. 缺失阶段补齐
    ensurePhaseExists(phaseMapper, task, TASK_CREATION, "任务创建阶段", 1, "running", now);
    ensurePhaseExists(phaseMapper, task, PREPARATION, "认定筹备阶段", 2, "pending", now);
    ensurePhaseExists(phaseMapper, task, ARCHIVE, "认定归档阶段", 3, "pending", now);
    ensurePhaseExists(phaseMapper, task, CERTIFICATION_COMPLETED, "认定完成", 4, "pending", now);

    // 5. 重新对齐阶段行状态，再按实际 completed 行数同步任务进度
    String currentPhase = task.getCurrentPhase();
    if (!CertificationTaskPhases.isValid(currentPhase)) {
      currentPhase = TASK_CREATION;
      task.setCurrentPhase(currentPhase);
    }
    realignPhaseStatuses(phaseMapper, taskId, currentPhase, now);
    task.setPhaseTotalCount(PHASES.size());
    task.setPhaseCompletedCount(countCompletedPhases(phaseMapper, taskId));
    task.setLastBusinessUpdatedAt(now);
    taskMapper.updateById(task);
  }

  private static boolean needsPhaseRepair(List<CertificationTaskPhaseEntity> phases) {
    if (phases == null || phases.isEmpty()) {
      return true;
    }
    boolean hasLegacyCode = phases.stream().anyMatch(p -> LEGACY_PHASE_CODES.contains(p.getPhaseCode()));
    long canonicalCount = phases.stream()
        .filter(p -> CertificationTaskPhases.PHASES.contains(p.getPhaseCode()))
        .count();
    return hasLegacyCode || canonicalCount < PHASES.size() || phases.size() > PHASES.size();
  }

  private static void ensurePhaseExists(
      CertificationTaskPhaseMapper phaseMapper,
      CertificationTaskEntity task,
      String phaseCode,
      String phaseName,
      int phaseOrder,
      String status,
      LocalDateTime now
  ) {
    if (findPhase(phaseMapper, task.getId(), phaseCode) != null) {
      return;
    }
    CertificationTaskPhaseEntity phase = new CertificationTaskPhaseEntity();
    phase.setTenantId(task.getTenantId() == null ? 1L : task.getTenantId());
    phase.setTaskId(task.getId());
    phase.setPhaseCode(phaseCode);
    phase.setPhaseName(phaseName);
    phase.setPhaseOrder(phaseOrder);
    phase.setStatus(status);
    phase.setDeleted(0);
    phase.setCreatedAt(now);
    phase.setUpdatedAt(now);
    phaseMapper.insert(phase);
  }

  private static void realignPhaseStatuses(
      CertificationTaskPhaseMapper phaseMapper,
      Long taskId,
      String currentPhase,
      LocalDateTime now
  ) {
    int targetOrder = CertificationTaskPhases.orderOf(currentPhase);
    boolean reachedFinal = CERTIFICATION_COMPLETED.equals(currentPhase);
    for (CertificationTaskPhaseEntity phase : listPhases(phaseMapper, taskId)) {
      Integer order = phase.getPhaseOrder();
      if (order == null) {
        continue;
      }
      if (order < targetOrder || reachedFinal) {
        // 任务创建阶段资料未齐时保持「待补充完整」，不因已进入筹备而强制 completed
        if (!reachedFinal
            && TASK_CREATION.equals(phase.getPhaseCode())
            && PHASE_STATUS_PENDING_COMPLETE.equals(phase.getStatus())) {
          if (phase.getStartedAt() == null) {
            phase.setStartedAt(now);
          }
          phase.setCompletedAt(null);
        } else {
          phase.setStatus("completed");
          if (phase.getStartedAt() == null) {
            phase.setStartedAt(now);
          }
          if (phase.getCompletedAt() == null) {
            phase.setCompletedAt(now);
          }
        }
      } else if (order.equals(targetOrder)) {
        if (!PHASE_STATUS_PENDING_COMPLETE.equals(phase.getStatus())) {
          phase.setStatus("running");
        }
        if (phase.getStartedAt() == null) {
          phase.setStartedAt(now);
        }
        phase.setCompletedAt(null);
      } else {
        phase.setStatus("pending");
        phase.setStartedAt(null);
        phase.setCompletedAt(null);
      }
      phaseMapper.updateById(phase);
    }
  }

  private static List<CertificationTaskPhaseEntity> listPhases(
      CertificationTaskPhaseMapper phaseMapper,
      Long taskId
  ) {
    return phaseMapper.selectList(new LambdaQueryWrapper<CertificationTaskPhaseEntity>()
        .eq(CertificationTaskPhaseEntity::getTaskId, taskId)
        .orderByAsc(CertificationTaskPhaseEntity::getPhaseOrder));
  }

  private static CertificationTaskPhaseEntity findPhase(
      CertificationTaskPhaseMapper phaseMapper,
      Long taskId,
      String phaseCode
  ) {
    return phaseMapper.selectOne(new LambdaQueryWrapper<CertificationTaskPhaseEntity>()
        .eq(CertificationTaskPhaseEntity::getTaskId, taskId)
        .eq(CertificationTaskPhaseEntity::getPhaseCode, phaseCode));
  }

  private static int countCompletedPhases(CertificationTaskPhaseMapper phaseMapper, Long taskId) {
    Long count = phaseMapper.selectCount(new LambdaQueryWrapper<CertificationTaskPhaseEntity>()
        .eq(CertificationTaskPhaseEntity::getTaskId, taskId)
        .eq(CertificationTaskPhaseEntity::getStatus, "completed"));
    return count == null ? 0 : count.intValue();
  }

  record PhaseSeed(String code, String name, Integer order, String status) {
  }
}
