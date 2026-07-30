package com.guoyu.platform.certification.scoring;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.certification.entity.CertificationAssessorTaskEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalScoringConfigEntity;
import com.guoyu.platform.certification.entity.CertificationPracticalStartSessionEntity;
import com.guoyu.platform.certification.info.entity.CertificationInfoAssessorEntity;
import com.guoyu.platform.certification.info.mapper.CertificationInfoAssessorMapper;
import com.guoyu.platform.certification.mapper.CertificationAssessorTaskMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalScoringConfigMapper;
import com.guoyu.platform.certification.mapper.CertificationPracticalStartSessionMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PracticalStartApplicationService {

  private final CertificationPracticalStartSessionMapper sessionMapper;
  private final CertificationPracticalScoringConfigMapper configMapper;
  private final CertificationAssessorTaskMapper assessorTaskMapper;
  private final CertificationCandidateMapper candidateMapper;
  private final CertificationInfoAssessorMapper assessorMapper;
  private final PracticalStartEventPublisher publisher;
  private final ObjectMapper objectMapper;
  private final AssessorWorkbenchContextService workbenchContextService;

  public PracticalStartApplicationService(
      CertificationPracticalStartSessionMapper sessionMapper,
      CertificationPracticalScoringConfigMapper configMapper,
      CertificationAssessorTaskMapper assessorTaskMapper,
      CertificationCandidateMapper candidateMapper,
      CertificationInfoAssessorMapper assessorMapper,
      PracticalStartEventPublisher publisher,
      ObjectMapper objectMapper,
      AssessorWorkbenchContextService workbenchContextService
  ) {
    this.sessionMapper = sessionMapper;
    this.configMapper = configMapper;
    this.assessorTaskMapper = assessorTaskMapper;
    this.candidateMapper = candidateMapper;
    this.assessorMapper = assessorMapper;
    this.publisher = publisher;
    this.objectMapper = objectMapper;
    this.workbenchContextService = workbenchContextService;
  }

  public void ensureSession(
      CertificationPracticalScoringConfigEntity config,
      Long candidateId
  ) {
    CertificationPracticalStartSessionEntity existing = sessionMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalStartSessionEntity>()
        .eq(CertificationPracticalStartSessionEntity::getTaskId, config.getTaskId())
        .eq(CertificationPracticalStartSessionEntity::getCandidateId, candidateId)
        .last("LIMIT 1"));
    if (existing != null) {
      boolean changed = !config.getId().equals(existing.getConfigId())
          || !config.getPaperId().equals(existing.getPaperId())
          || !config.getGroupId().equals(existing.getGroupId());
      if (!changed) {
        return;
      }
      if (!Set.of("waiting", "ready").contains(existing.getStatus())) {
        throw new BusinessException(ErrorCode.CONFLICT, "考生实操考试已开始或已完成，不能替换评分表");
      }
      existing.setConfigId(config.getId());
      existing.setPaperId(config.getPaperId());
      existing.setGroupId(config.getGroupId());
      existing.setCandidateConfirmedAt(null);
      existing.setCandidateConfirmedBy(null);
      existing.setAssessorConfirmationsJson("{}");
      existing.setStatus("waiting");
      existing.setVersionNo(existing.getVersionNo() == null ? 1 : existing.getVersionNo() + 1);
      sessionMapper.updateById(existing);
      return;
    }
    CertificationPracticalStartSessionEntity session = new CertificationPracticalStartSessionEntity();
    session.setTaskId(config.getTaskId());
    session.setCandidateId(candidateId);
    session.setConfigId(config.getId());
    session.setPaperId(config.getPaperId());
    session.setGroupId(config.getGroupId());
    session.setAssessorConfirmationsJson("{}");
    session.setStatus("waiting");
    session.setVersionNo(0);
    session.setDeleted(0);
    sessionMapper.insert(session);
  }

  @Transactional(readOnly = true)
  public PracticalScoringModels.StartSessionView forAssessorTask(Long assessorTaskId) {
    CertificationAssessorTaskEntity task = requireOwnedTask(assessorTaskId);
    return view(requireSession(task.getTaskId(), task.getCandidateId()));
  }

  @Transactional
  public PracticalScoringModels.StartSessionView startByAssessorTask(Long assessorTaskId) {
    CertificationAssessorTaskEntity task = requireOwnedTask(assessorTaskId);
    return startCandidate(task.getTaskId(), task.getCandidateId());
  }

  @Transactional(readOnly = true)
  public PracticalScoringModels.StartSessionView session(Long taskId, Long candidateId) {
    CertificationPracticalStartSessionEntity session = sessionMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalStartSessionEntity>()
            .eq(CertificationPracticalStartSessionEntity::getTaskId, taskId)
            .eq(CertificationPracticalStartSessionEntity::getCandidateId, candidateId)
            .last("LIMIT 1"));
    return session == null ? null : view(session);
  }

  @Transactional
  public PracticalScoringModels.StartSessionView confirmCandidate(Long taskId, Long candidateId) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationPracticalStartSessionEntity session =
        lockSession(user.getTenantId(), taskId, candidateId);
    validateGeneratedTasks(session);
    if ("completed".equals(session.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操考试已完成，不能重复确认");
    }
    if (session.getCandidateConfirmedAt() == null) {
      session.setCandidateConfirmedAt(LocalDateTime.now());
      session.setCandidateConfirmedBy(user.getUserId());
      session.setVersionNo(session.getVersionNo() + 1);
    }
    return evaluateAndPublish(session, user.getUserId());
  }

  @Transactional
  public PracticalScoringModels.StartSessionView startCandidate(Long taskId, Long candidateId) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationPracticalStartSessionEntity session =
        lockSession(user.getTenantId(), taskId, candidateId);
    requireLeader(session);
    validateGeneratedTasks(session);
    if ("completed".equals(session.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操考试已完成，不能重复开始");
    }
    Map<String, String> confirmations = confirmations(session);
    Set<Long> expected = expectedAssessors(session, validateGeneratedTasks(session));
    long confirmedCount = confirmations.keySet().stream()
        .map(Long::valueOf)
        .filter(expected::contains)
        .count();
    // 业务规则：三名考评员全部确认即可开始，无需考生单独确认
    if (confirmedCount != 3) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "三名考评员必须全部确认后，组长才能开始考试");
    }
    List<CertificationPracticalStartSessionEntity> activeSessions =
        sessionMapper.lockStartedSessionsByGroup(user.getTenantId(), taskId, session.getGroupId());
    boolean anotherCandidateRunning = activeSessions.stream()
        .anyMatch(active -> !candidateId.equals(active.getCandidateId()));
    if (anotherCandidateRunning) {
      throw new BusinessException(ErrorCode.CONFLICT, "同一考评组已有考生处于考试中");
    }
    CertificationCandidateEntity candidate = candidateMapper.selectById(candidateId);
    if (candidate == null || !taskId.equals(candidate.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考生不存在");
    }
    if (!"started".equals(session.getStatus())) {
      session.setStatus("started");
      session.setStartedAt(LocalDateTime.now());
      session.setStartedBy(user.getUserId());
      session.setVersionNo(session.getVersionNo() + 1);
      candidate.setStatus("exam_in_progress");
      candidateMapper.updateById(candidate);
    }
    sessionMapper.updateById(session);
    PracticalScoringModels.StartSessionView event = view(session);
    publishAfterCommit(session.getTenantId(), broadcastView(session));
    return event;
  }

  @Transactional
  public PracticalScoringModels.StartSessionView confirmAssessor(Long assessorTaskId) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationAssessorTaskEntity task = requireOwnedTask(assessorTaskId);
    CertificationInfoAssessorEntity assessor = currentAssessor();
    CertificationPracticalStartSessionEntity session =
        lockSession(user.getTenantId(), task.getTaskId(), task.getCandidateId());
    Map<String, String> confirmations = confirmations(session);
    confirmations.put(String.valueOf(assessor.getId()), LocalDateTime.now().toString());
    session.setAssessorConfirmationsJson(write(confirmations));
    session.setVersionNo(session.getVersionNo() + 1);
    return evaluateAndPublish(session, user.getUserId());
  }

  @Transactional(readOnly = true)
  public void requireStarted(Long taskId, Long candidateId) {
    CertificationPracticalStartSessionEntity session = requireSession(taskId, candidateId);
    if (!"started".equals(session.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "考生与三名考评员尚未完成确认，不能开始评分");
    }
  }

  @Transactional(readOnly = true)
  public boolean isCompleted(Long taskId, Long candidateId) {
    return "completed".equals(requireSession(taskId, candidateId).getStatus());
  }

  /**
   * Removes a waiting or ready unified-start session as part of cancelling generated scoring tasks.
   */
  @Transactional
  public void cancelUnstartedSession(Long taskId, Long candidateId) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationPracticalStartSessionEntity session = lockSession(user.getTenantId(), taskId, candidateId);
    if (!Set.of("waiting", "ready").contains(session.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "实操考试已开始或已完成，不能取消评分任务");
    }
    sessionMapper.deleteById(session.getId());
  }

  @Transactional
  public PracticalScoringModels.StartSessionView completeCandidate(
      Long taskId,
      Long candidateId,
      String summaryFileName
  ) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationPracticalStartSessionEntity session =
        lockSession(user.getTenantId(), taskId, candidateId);
    requireLeader(session);
    if (!"started".equals(session.getStatus())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "只有考试中的考生才能完成考试");
    }
    CertificationCandidateEntity candidate = candidateMapper.selectById(candidateId);
    if (candidate == null || !taskId.equals(candidate.getTaskId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考生不存在");
    }
    LocalDateTime now = LocalDateTime.now();
    session.setStatus("completed");
    session.setCompletedAt(now);
    session.setCompletedBy(user.getUserId());
    session.setSummaryGeneratedAt(now);
    session.setSummaryFileName(summaryFileName);
    session.setVersionNo(session.getVersionNo() + 1);
    sessionMapper.updateById(session);
    candidate.setStatus("completed");
    candidateMapper.updateById(candidate);
    PracticalScoringModels.StartSessionView event = view(session);
    publishAfterCommit(session.getTenantId(), broadcastView(session));
    return event;
  }

  @Transactional
  public PracticalScoringModels.StartSessionView markAbsent(Long taskId, Long candidateId) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationPracticalStartSessionEntity session =
        lockSession(user.getTenantId(), taskId, candidateId);
    if ("completed".equals(session.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "completed practical session cannot be marked absent");
    }
    if (!"absent".equals(session.getStatus())) {
      session.setStatus("absent");
      session.setCompletedAt(LocalDateTime.now());
      session.setCompletedBy(user.getUserId());
      session.setVersionNo(session.getVersionNo() == null ? 1 : session.getVersionNo() + 1);
      sessionMapper.updateById(session);
    }
    PracticalScoringModels.StartSessionView event = view(session);
    publishAfterCommit(session.getTenantId(), broadcastView(session));
    return event;
  }

  private PracticalScoringModels.StartSessionView evaluateAndPublish(
      CertificationPracticalStartSessionEntity session,
      Long operatorId
  ) {
    CertificationPracticalScoringConfigEntity config = configMapper.selectById(session.getConfigId());
    if (config == null
        || !session.getPaperId().equals(config.getPaperId())
        || !session.getGroupId().equals(config.getGroupId())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "当天实操试卷或考评组匹配失败");
    }
    List<CertificationAssessorTaskEntity> tasks = validateGeneratedTasks(session);
    Set<Long> expected = expectedAssessors(session, tasks);
    Set<Long> confirmed = confirmations(session).keySet().stream()
        .map(Long::valueOf)
        .filter(expected::contains)
        .collect(Collectors.toSet());
    // 业务规则：三名考评员全部确认即可进入就绪态，无需考生单独确认
    if (confirmed.size() == 3 && "waiting".equals(session.getStatus())) {
      session.setStatus("ready");
    }
    sessionMapper.updateById(session);
    PracticalScoringModels.StartSessionView event = view(session);
    publishAfterCommit(session.getTenantId(), broadcastView(session));
    return event;
  }

  private void publishAfterCommit(Long tenantId, PracticalScoringModels.StartSessionView event) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          publisher.publish(tenantId, event);
        }
      });
    } else {
      publisher.publish(tenantId, event);
    }
  }

  private CertificationAssessorTaskEntity requireOwnedTask(Long id) {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationInfoAssessorEntity assessor = assessorMapper.selectOne(
        new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
            .eq(CertificationInfoAssessorEntity::getUserId, user.getUserId())
            .eq(CertificationInfoAssessorEntity::getAuditStatus, "approved")
            .last("LIMIT 1"));
    CertificationAssessorTaskEntity task = assessorTaskMapper.selectById(id);
    if (assessor == null || task == null || !assessor.getId().equals(task.getAssessorId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该统一开考会话");
    }
    workbenchContextService.requireAccessibleTask(assessor, task.getTaskId());
    return task;
  }

  private CertificationPracticalStartSessionEntity lockSession(
      Long tenantId,
      Long taskId,
      Long candidateId
  ) {
    CertificationPracticalStartSessionEntity session =
        sessionMapper.lockSession(tenantId, taskId, candidateId);
    if (session == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "统一开考会话不存在，请先生成评分任务");
    }
    return session;
  }

  private CertificationPracticalStartSessionEntity requireSession(Long taskId, Long candidateId) {
    CertificationPracticalStartSessionEntity session = sessionMapper.selectOne(
        new LambdaQueryWrapper<CertificationPracticalStartSessionEntity>()
            .eq(CertificationPracticalStartSessionEntity::getTaskId, taskId)
            .eq(CertificationPracticalStartSessionEntity::getCandidateId, candidateId)
            .last("LIMIT 1"));
    if (session == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "统一开考会话不存在，请先生成评分任务");
    }
    return session;
  }

  private PracticalScoringModels.StartSessionView view(
      CertificationPracticalStartSessionEntity session
  ) {
    Map<String, String> confirmations = confirmations(session);
    Long currentAssessorId = currentAssessorIdOrNull();
    return new PracticalScoringModels.StartSessionView(
        session.getId(), session.getTaskId(), session.getCandidateId(), session.getPaperId(),
        session.getStatus(), session.getCandidateConfirmedAt() != null, confirmations.size(),
        3, currentAssessorId != null && confirmations.containsKey(String.valueOf(currentAssessorId)),
        session.getVersionNo(), session.getStartedAt(), session.getCompletedAt(),
        session.getSummaryFileName());
  }

  private PracticalScoringModels.StartSessionView broadcastView(
      CertificationPracticalStartSessionEntity session
  ) {
    Map<String, String> confirmations = confirmations(session);
    return new PracticalScoringModels.StartSessionView(
        session.getId(), session.getTaskId(), session.getCandidateId(), session.getPaperId(),
        session.getStatus(), session.getCandidateConfirmedAt() != null, confirmations.size(),
        3, false, session.getVersionNo(), session.getStartedAt(), session.getCompletedAt(),
        session.getSummaryFileName());
  }

  private void requireLeader(CertificationPracticalStartSessionEntity session) {
    CertificationInfoAssessorEntity assessor = currentAssessor();
    List<CertificationAssessorTaskEntity> tasks = assessorTasks(session);
    Long leaderAssessorId = tasks.stream()
        .map(CertificationAssessorTaskEntity::getLeaderAssessorId)
        .filter(id -> id != null)
        .findFirst()
        .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "考评组未设置组长"));
    if (!leaderAssessorId.equals(assessor.getId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "只有本组组长可以开始或完成考试");
    }
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

  private Long currentAssessorIdOrNull() {
    PlatformUserPrincipal user = SecurityContext.currentUser();
    CertificationInfoAssessorEntity assessor = assessorMapper.selectOne(
        new LambdaQueryWrapper<CertificationInfoAssessorEntity>()
            .eq(CertificationInfoAssessorEntity::getUserId, user.getUserId())
            .eq(CertificationInfoAssessorEntity::getAuditStatus, "approved")
            .last("LIMIT 1"));
    return assessor == null ? null : assessor.getId();
  }

  private List<CertificationAssessorTaskEntity> validateGeneratedTasks(
      CertificationPracticalStartSessionEntity session
  ) {
    List<CertificationAssessorTaskEntity> tasks = assessorTasks(session);
    Set<Long> expected = expectedAssessors(session, tasks);
    if (expected.size() != 3 || tasks.stream().anyMatch(task -> !session.getPaperId().equals(task.getPaperId()))) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "评分任务未完整匹配当天唯一试卷和三名考评员");
    }
    return tasks;
  }

  private Set<Long> expectedAssessors(
      CertificationPracticalStartSessionEntity session,
      List<CertificationAssessorTaskEntity> tasks
  ) {
    return tasks.stream()
        .filter(task -> session.getPaperId().equals(task.getPaperId()))
        .map(CertificationAssessorTaskEntity::getAssessorId)
        .collect(Collectors.toSet());
  }

  private List<CertificationAssessorTaskEntity> assessorTasks(
      CertificationPracticalStartSessionEntity session
  ) {
    return assessorTaskMapper.selectList(
        new LambdaQueryWrapper<CertificationAssessorTaskEntity>()
            .eq(CertificationAssessorTaskEntity::getTaskId, session.getTaskId())
            .eq(CertificationAssessorTaskEntity::getCandidateId, session.getCandidateId()));
  }

  private Map<String, String> confirmations(CertificationPracticalStartSessionEntity session) {
    try {
      if (session.getAssessorConfirmationsJson() == null) {
        return new LinkedHashMap<>();
      }
      return objectMapper.readValue(session.getAssessorConfirmationsJson(), new TypeReference<>() {
      });
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "开考确认记录损坏");
    }
  }

  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "开考确认记录保存失败");
    }
  }
}
