package com.guoyu.platform.certification.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.certification.entity.CertificationAssessorWorkbenchContextEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalStartSessionEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorEntity;
import com.guoyu.platform.certification.entity.CertificationTaskAssessorGroupEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorWorkbenchContextMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalStartSessionMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorGroupMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the leader-owned tablet task context shared by the leader's members. */
@Service
@Slf4j
public class AssessorWorkbenchContextService {

  private final CertificationInfoAssessorMapper assessorMapper;
  private final CertificationTaskAssessorMapper taskAssessorMapper;
  private final CertificationTaskAssessorGroupMapper groupMapper;
  private final CertificationTaskMapper taskMapper;
  private final CertificationAssessorWorkbenchContextMapper contextMapper;
  private final CertificationPracticalStartSessionMapper sessionMapper;

  public AssessorWorkbenchContextService(
      CertificationInfoAssessorMapper assessorMapper,
      CertificationTaskAssessorMapper taskAssessorMapper,
      CertificationTaskAssessorGroupMapper groupMapper,
      CertificationTaskMapper taskMapper,
      CertificationAssessorWorkbenchContextMapper contextMapper,
      CertificationPracticalStartSessionMapper sessionMapper
  ) {
    this.assessorMapper = assessorMapper;
    this.taskAssessorMapper = taskAssessorMapper;
    this.groupMapper = groupMapper;
    this.taskMapper = taskMapper;
    this.contextMapper = contextMapper;
    this.sessionMapper = sessionMapper;
  }

  @Transactional(readOnly = true)
  public PracticalScoringModels.AssessorWorkbenchContext current() {
    return view(currentAssessor());
  }

  @Transactional
  public PracticalScoringModels.AssessorWorkbenchContext switchTask(Long taskId) {
    CertificationInfoAssessorEntity assessor = currentAssessor();
    ResolvedContext resolved = resolve(assessor);
    if (!resolved.leader()) {
      log.warn("考评员{}尝试切换组长工作台任务", assessor.getId());
      throw new BusinessException(ErrorCode.FORBIDDEN, "只有考评组长可以切换认定任务");
    }
    boolean selectable = resolved.bindings().stream()
        .anyMatch(binding -> Objects.equals(binding.task().getId(), taskId)
            && Objects.equals(binding.group().getLeaderAssessorId(), assessor.getId()));
    if (!selectable) {
      log.warn("组长{}尝试切换未绑定任务{}", assessor.getId(), taskId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权切换到该认定任务");
    }
    CertificationAssessorWorkbenchContextEntity context = contextMapper.selectOne(
        new LambdaQueryWrapper<CertificationAssessorWorkbenchContextEntity>()
            .eq(CertificationAssessorWorkbenchContextEntity::getLeaderAssessorId, assessor.getId())
            .last("LIMIT 1"));
    if (context == null) {
      context = new CertificationAssessorWorkbenchContextEntity();
      context.setLeaderAssessorId(assessor.getId());
      context.setActiveTaskId(taskId);
      context.setDeleted(0);
      contextMapper.insert(context);
    } else {
      context.setActiveTaskId(taskId);
      contextMapper.updateById(context);
    }
    return view(assessor);
  }

  /** Enforces both an active member binding and the leader-selected task. */
  @Transactional(readOnly = true)
  public void requireAccessibleTask(CertificationInfoAssessorEntity assessor, Long taskId) {
    ResolvedContext resolved = resolve(assessor);
    boolean bound = resolved.bindings().stream()
        .anyMatch(binding -> Objects.equals(binding.task().getId(), taskId));
    if (!bound || !Objects.equals(resolved.activeTaskId(), taskId)) {
      log.warn("考评员{}尝试访问非当前或已解绑的认定任务{}", assessor.getId(), taskId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前组长未指定该认定任务或您已被解绑");
    }
  }

  private PracticalScoringModels.AssessorWorkbenchContext view(CertificationInfoAssessorEntity assessor) {
    ResolvedContext resolved = resolve(assessor);
    List<PracticalScoringModels.AssessorWorkbenchTask> tasks = resolved.leader()
        ? resolved.bindings().stream()
            .filter(binding -> Objects.equals(binding.group().getLeaderAssessorId(), assessor.getId()))
            .map(binding -> taskView(binding.task()))
            .distinct()
            .sorted(Comparator.comparing(PracticalScoringModels.AssessorWorkbenchTask::taskName,
                Comparator.nullsLast(String::compareTo)))
            .toList()
        : List.of();
    String leaderName = resolved.leader() ? assessor.getAssessorName() : resolved.leaderName();
    return new PracticalScoringModels.AssessorWorkbenchContext(
        resolved.leader(), resolved.leaderAssessorId(), leaderName, resolved.activeTaskId(), tasks);
  }

  private PracticalScoringModels.AssessorWorkbenchTask taskView(CertificationTaskEntity task) {
    int started = Math.toIntExact(sessionMapper.selectCount(
        new LambdaQueryWrapper<CertificationPracticalStartSessionEntity>()
            .eq(CertificationPracticalStartSessionEntity::getTaskId, task.getId())
            .eq(CertificationPracticalStartSessionEntity::getStatus, "started")));
    return new PracticalScoringModels.AssessorWorkbenchTask(
        task.getId(), task.getTaskNo(), task.getTaskName(), task.getExamDate(), started);
  }

  private ResolvedContext resolve(CertificationInfoAssessorEntity assessor) {
    List<CertificationTaskAssessorEntity> memberships = taskAssessorMapper.selectList(
        new LambdaQueryWrapper<CertificationTaskAssessorEntity>()
            .eq(CertificationTaskAssessorEntity::getAssessorId, assessor.getId())
            .isNotNull(CertificationTaskAssessorEntity::getGroupId));
    if (memberships == null || memberships.isEmpty()) {
      return ResolvedContext.empty();
    }
    List<Long> groupIds = memberships.stream().map(CertificationTaskAssessorEntity::getGroupId).distinct().toList();
    Map<Long, CertificationTaskAssessorGroupEntity> groups = byId(groupMapper.selectBatchIds(groupIds));
    List<Long> taskIds = memberships.stream().map(CertificationTaskAssessorEntity::getTaskId).distinct().toList();
    Map<Long, CertificationTaskEntity> tasks = byId(taskMapper.selectBatchIds(taskIds));
    List<TaskBinding> bindings = memberships.stream()
        .map(member -> new TaskBinding(member, groups.get(member.getGroupId()), tasks.get(member.getTaskId())))
        .filter(binding -> binding.group() != null && binding.group().getLeaderAssessorId() != null
            && binding.task() != null && !"completed".equals(binding.task().getStatus()))
        .toList();
    if (bindings.isEmpty()) {
      return ResolvedContext.empty();
    }
    List<Long> leaderIds = bindings.stream().map(binding -> binding.group().getLeaderAssessorId()).distinct().toList();
    if (leaderIds.size() != 1) {
      log.warn("考评员{}存在多个未完成任务组长{}", assessor.getId(), leaderIds);
      throw new BusinessException(ErrorCode.CONFLICT, "考评员存在多个组长，请先完成或调整原任务分组");
    }
    Long leaderId = leaderIds.getFirst();
    boolean leader = Objects.equals(leaderId, assessor.getId());
    CertificationAssessorWorkbenchContextEntity stored = contextMapper.selectOne(
        new LambdaQueryWrapper<CertificationAssessorWorkbenchContextEntity>()
            .eq(CertificationAssessorWorkbenchContextEntity::getLeaderAssessorId, leaderId)
            .last("LIMIT 1"));
    List<CertificationTaskEntity> selectable = bindings.stream()
        .filter(binding -> Objects.equals(binding.group().getLeaderAssessorId(), leaderId))
        .map(TaskBinding::task).distinct()
        .sorted(Comparator.comparing(CertificationTaskEntity::getUpdatedAt,
            Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
    Long activeTaskId = stored != null && selectable.stream().anyMatch(task -> Objects.equals(task.getId(), stored.getActiveTaskId()))
        ? stored.getActiveTaskId()
        : selectable.getFirst().getId();
    String leaderName = leader ? assessor.getAssessorName() : groups.get(bindings.getFirst().member().getGroupId()) == null
        ? null : loadAssessorName(leaderId);
    return new ResolvedContext(leader, leaderId, leaderName, activeTaskId, bindings);
  }

  private String loadAssessorName(Long assessorId) {
    CertificationInfoAssessorEntity assessor = assessorMapper.selectById(assessorId);
    return assessor == null ? null : assessor.getAssessorName();
  }

  private CertificationInfoAssessorEntity currentAssessor() {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationInfoAssessorEntity assessor = assessorMapper.selectOne(
        new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
            .eq(CertificationInfoAssessorEntity::getUserId, user.getUserId())
            .eq(CertificationInfoAssessorEntity::getAuditStatus, "approved")
            .last("LIMIT 1"));
    if (assessor == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号未绑定有效考评员档案");
    }
    return assessor;
  }

  private static <T extends com.guoyu.platform.common.domain.BaseEntity> Map<Long, T> byId(List<T> values) {
    Map<Long, T> result = new HashMap<>();
    if (values != null) {
      values.stream().filter(Objects::nonNull).filter(value -> value.getId() != null)
          .forEach(value -> result.put(value.getId(), value));
    }
    return result;
  }

  private record TaskBinding(
      CertificationTaskAssessorEntity member,
      CertificationTaskAssessorGroupEntity group,
      CertificationTaskEntity task
  ) {
  }

  private record ResolvedContext(
      boolean leader,
      Long leaderAssessorId,
      String leaderName,
      Long activeTaskId,
      List<TaskBinding> bindings
  ) {
    private static ResolvedContext empty() {
      return new ResolvedContext(false, null, null, null, List.of());
    }
  }
}
