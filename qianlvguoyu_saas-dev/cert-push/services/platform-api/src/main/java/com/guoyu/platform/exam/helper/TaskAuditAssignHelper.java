package com.guoyu.platform.exam.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guoyu.platform.exam.entity.ExamTaskExamAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamshicaoEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamsubAuditRecordEntity;
import com.guoyu.platform.exam.entity.ExamTaskExamsubEntity;
import com.guoyu.platform.exam.service.ExamWorkTypeApplicationService;
import com.guoyu.platform.exam.vo.ExamWorkTypeVO;
import com.guoyu.platform.system.entity.UserEntity;
import com.guoyu.platform.system.mapper.UserMapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 题库审核指派公共 helper —— 沿用 ExamTaskXimuApplicationService 的方法切分。
 * <p>
 * 1) autoCompleteStatus —— 每次列表查询触发:把"终审中(status=2)"且终审审计记录数 >= nums 的任务推到 status=3。
 * 2) loadWorkTypeNames / loadUserDisplayNames —— 列表富化。
 * <p>
 * 三个 service 复用本类,以 tag ∈ {exam/examshicao/examsub} 区分 mapper 走哪条分支。
 */
@Component
@Slf4j
public class TaskAuditAssignHelper {

  private final ExamWorkTypeApplicationService workTypeService;
  private final UserMapper userMapper;

  public TaskAuditAssignHelper(ExamWorkTypeApplicationService workTypeService,
                               UserMapper userMapper) {
    this.workTypeService = workTypeService;
    this.userMapper = userMapper;
  }

  // ─────────────────── autoCompleteStatus(理论) ───────────────────

  public static int autoCompleteStatus(Integer tenantId,
                                       BaseMapper<ExamTaskExamEntity> taskMapper,
                                       BaseMapper<ExamTaskExamAuditRecordEntity> auditMapper) {
    if (taskMapper == null || auditMapper == null) return 0;
    List<ExamTaskExamEntity> inProgress = taskMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamEntity>()
            .eq(ExamTaskExamEntity::getTenantId, tenantId)
            .eq(ExamTaskExamEntity::getStatus, 2));
    if (inProgress.isEmpty()) return 0;
    Set<Integer> taskIds = inProgress.stream().map(ExamTaskExamEntity::getId).collect(Collectors.toSet());
    List<ExamTaskExamAuditRecordEntity> audits = auditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamAuditRecordEntity>()
            .eq(ExamTaskExamAuditRecordEntity::getTenantId, tenantId)
            .in(ExamTaskExamAuditRecordEntity::getTaskId, taskIds)
            .eq(ExamTaskExamAuditRecordEntity::getAuditRole, 1)
            .eq(ExamTaskExamAuditRecordEntity::getAuditStatus, 1));
    Map<Integer, Long> completed = audits.stream()
        .collect(Collectors.groupingBy(ExamTaskExamAuditRecordEntity::getTaskId, Collectors.counting()));
    int updated = 0;
    for (ExamTaskExamEntity e : inProgress) {
      long cnt = completed.getOrDefault(e.getId(), 0L);
      if (cnt >= e.getNums()) {
        e.setStatus(3);
        taskMapper.updateById(e);
        updated++;
        log.info("Auto-completed exam task id={}", e.getId());
      }
    }
    return updated;
  }

  // ─────────────────── autoCompleteStatus(实操) ───────────────────

  public static int autoCompleteStatusExamshicao(Integer tenantId,
                                                 BaseMapper<ExamTaskExamshicaoEntity> taskMapper,
                                                 BaseMapper<ExamTaskExamshicaoAuditRecordEntity> auditMapper) {
    if (taskMapper == null || auditMapper == null) return 0;
    List<ExamTaskExamshicaoEntity> inProgress = taskMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamshicaoEntity>()
            .eq(ExamTaskExamshicaoEntity::getTenantId, tenantId)
            .eq(ExamTaskExamshicaoEntity::getStatus, 2));
    if (inProgress.isEmpty()) return 0;
    Set<Integer> taskIds = inProgress.stream().map(ExamTaskExamshicaoEntity::getId).collect(Collectors.toSet());
    List<ExamTaskExamshicaoAuditRecordEntity> audits = auditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamshicaoAuditRecordEntity>()
            .eq(ExamTaskExamshicaoAuditRecordEntity::getTenantId, tenantId)
            .in(ExamTaskExamshicaoAuditRecordEntity::getTaskId, taskIds)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditRole, 1)
            .eq(ExamTaskExamshicaoAuditRecordEntity::getAuditStatus, 1));
    Map<Integer, Long> completed = audits.stream()
        .collect(Collectors.groupingBy(ExamTaskExamshicaoAuditRecordEntity::getTaskId, Collectors.counting()));
    int updated = 0;
    for (ExamTaskExamshicaoEntity e : inProgress) {
      long cnt = completed.getOrDefault(e.getId(), 0L);
      if (cnt >= e.getNums()) {
        e.setStatus(3);
        taskMapper.updateById(e);
        updated++;
        log.info("Auto-completed examshicao task id={}", e.getId());
      }
    }
    return updated;
  }

  // ─────────────────── autoCompleteStatus(主观) ───────────────────

  public static int autoCompleteStatusExamsub(Integer tenantId,
                                              BaseMapper<ExamTaskExamsubEntity> taskMapper,
                                              BaseMapper<ExamTaskExamsubAuditRecordEntity> auditMapper) {
    if (taskMapper == null || auditMapper == null) return 0;
    List<ExamTaskExamsubEntity> inProgress = taskMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamsubEntity>()
            .eq(ExamTaskExamsubEntity::getTenantId, tenantId)
            .eq(ExamTaskExamsubEntity::getStatus, 2));
    if (inProgress.isEmpty()) return 0;
    Set<Integer> taskIds = inProgress.stream().map(ExamTaskExamsubEntity::getId).collect(Collectors.toSet());
    List<ExamTaskExamsubAuditRecordEntity> audits = auditMapper.selectList(
        new LambdaQueryWrapper<ExamTaskExamsubAuditRecordEntity>()
            .eq(ExamTaskExamsubAuditRecordEntity::getTenantId, tenantId)
            .in(ExamTaskExamsubAuditRecordEntity::getTaskId, taskIds)
            .eq(ExamTaskExamsubAuditRecordEntity::getAuditRole, 1)
            .eq(ExamTaskExamsubAuditRecordEntity::getAuditStatus, 1));
    Map<Integer, Long> completed = audits.stream()
        .collect(Collectors.groupingBy(ExamTaskExamsubAuditRecordEntity::getTaskId, Collectors.counting()));
    int updated = 0;
    for (ExamTaskExamsubEntity e : inProgress) {
      long cnt = completed.getOrDefault(e.getId(), 0L);
      if (cnt >= e.getNums()) {
        e.setStatus(3);
        taskMapper.updateById(e);
        updated++;
        log.info("Auto-completed examsub task id={}", e.getId());
      }
    }
    return updated;
  }

  // ─────────────────── 字典加载 ───────────────────

  public Map<Integer, String> loadWorkTypeNames(Set<Integer> ids) {
    Map<Integer, String> result = new LinkedHashMap<>();
    if (ids == null || ids.isEmpty()) return result;
    for (Integer id : ids) {
      try {
        ExamWorkTypeVO vo = workTypeService.getById(id);
        result.put(id, vo == null || vo.getTitle() == null ? "-" : vo.getTitle());
      } catch (Exception e) {
        // 不能静默吞:之前 helper 把异常吃掉,导致事务被标记 rollback-only,
        // commit 时抛 UnexpectedRollbackException 看不到根因。这里 warn + 继续。
        log.warn("loadWorkTypeNames({}) failed: {}", id, e.toString());
        result.put(id, "-");
      }
    }
    return result;
  }

  public Map<Integer, String> loadUserDisplayNames(Collection<Integer> ids) {
    Map<Integer, String> result = new LinkedHashMap<>();
    if (ids == null || ids.isEmpty()) return result;
    List<UserEntity> users = userMapper.selectBatchIds(
        ids.stream().map(Integer::longValue).collect(Collectors.toSet()));
    for (UserEntity u : users) {
      String name = u.getDisplayName() != null ? u.getDisplayName() : u.getUsername();
      if (name == null) name = "***";
      result.put(u.getId().intValue(), name);
    }
    return result;
  }
}
