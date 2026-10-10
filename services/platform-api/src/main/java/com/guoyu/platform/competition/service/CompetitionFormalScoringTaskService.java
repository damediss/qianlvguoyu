package com.guoyu.platform.competition.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalScoringTaskEntity;
import com.guoyu.platform.competition.enums.CompetitionFormalCandidateExamStatus;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalScoringTaskMapper;
import com.guoyu.platform.competition.messaging.config.CompetitionScoringMessagingProperties;
import com.guoyu.platform.competition.messaging.entity.CompetitionOutboxEventEntity;
import com.guoyu.platform.competition.messaging.mapper.CompetitionOutboxEventMapper;
import com.guoyu.platform.competition.messaging.model.FormalExamScoringRequestedEvent;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionFormalScoringTaskService {
  public static final String CHANNEL = "scoring";
  public static final String EVENT_TYPE = "competition.formal-score.requested.v1";

  private final CompetitionFormalScoringTaskMapper taskMapper;
  private final CompetitionOutboxEventMapper outboxMapper;
  private final CompetitionFormalExamSessionMapper sessionMapper;
  private final CompetitionFormalCandidateExamMapper candidateMapper;
  private final CompetitionScoringMessagingProperties properties;
  private final ObjectMapper objectMapper;

  public CompetitionFormalScoringTaskService(CompetitionFormalScoringTaskMapper taskMapper,
      CompetitionOutboxEventMapper outboxMapper, CompetitionFormalExamSessionMapper sessionMapper,
      CompetitionFormalCandidateExamMapper candidateMapper,
      CompetitionScoringMessagingProperties properties, ObjectMapper objectMapper) {
    this.taskMapper = taskMapper;
    this.outboxMapper = outboxMapper;
    this.sessionMapper = sessionMapper;
    this.candidateMapper = candidateMapper;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public CompetitionFormalScoringTaskEntity enqueueInitial(
      CompetitionFormalCandidateExamEntity candidate, String reason, Long operatorId) {
    return enqueue(candidate, 1, reason, operatorId);
  }

  @Transactional
  public CompetitionFormalScoringTaskEntity enqueueRevision(Long candidateExamId,
      int revisionNo, String reason, Long operatorId) {
    CompetitionFormalCandidateExamEntity candidate = candidateMapper.selectById(candidateExamId);
    if (candidate == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手考试实例不存在");
    }
    if (revisionNo > 1 && (reason == null || reason.isBlank())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "重新判分必须填写原因");
    }
    return enqueue(candidate, revisionNo, reason, operatorId);
  }

  private CompetitionFormalScoringTaskEntity enqueue(CompetitionFormalCandidateExamEntity candidate,
      int revisionNo, String reason, Long operatorId) {
    if (candidate == null || candidate.getId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "选手考试实例不能为空");
    }
    CompetitionFormalCandidateExamStatus status =
        CompetitionFormalCandidateExamStatus.fromCode(candidate.getStatus());
    if (status == null || !status.isClosed()) {
      throw new BusinessException(ErrorCode.CONFLICT, "选手考试尚未到达终态");
    }
    CompetitionFormalScoringTaskEntity existing =
        taskMapper.selectByCandidateAndRevision(candidate.getId(), revisionNo);
    if (existing != null) {
      return existing;
    }
    LocalDateTime now = LocalDateTime.now();
    CompetitionFormalScoringTaskEntity task = new CompetitionFormalScoringTaskEntity();
    task.setId(IdWorker.getId());
    task.setTenantId(candidate.getTenantId());
    task.setExamSessionId(candidate.getExamSessionId());
    task.setCandidateExamId(candidate.getId());
    task.setResultRevisionNo(revisionNo);
    task.setTaskStatus("pending");
    task.setAttemptCount(0);
    task.setNextAttemptAt(now);
    task.setRequestedReason(reason);
    task.setRequestedBy(operatorId);
    task.setCreatedBy(operatorId);
    task.setUpdatedBy(operatorId);
    task.setCreatedAt(now);
    task.setUpdatedAt(now);
    task.setDeleted(0);
    if (taskMapper.insert(task) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "判分任务创建失败");
    }
    enqueueOutbox(task, 0, now, operatorId);
    CompetitionFormalExamSessionEntity session = sessionMapper.selectById(candidate.getExamSessionId());
    if (session != null && !"scoring".equals(session.getScoringStatus())) {
      session.setScoringStatus("queued");
      session.setScoringError(null);
      session.setUpdatedAt(now);
      session.setUpdatedBy(operatorId);
      sessionMapper.updateById(session);
    }
    return task;
  }

  @Transactional
  public CompetitionFormalScoringTaskEntity claim(Long taskId, String owner) {
    LocalDateTime now = LocalDateTime.now();
    if (taskMapper.claim(taskId, owner, now.plusSeconds(properties.getLeaseSeconds()), now) != 1) {
      return null;
    }
    return taskMapper.selectById(taskId);
  }

  @Transactional
  public void complete(CompetitionFormalScoringTaskEntity task, String owner) {
    LocalDateTime now = LocalDateTime.now();
    if (taskMapper.complete(task.getId(), owner, now) != 1) {
      throw new IllegalStateException("判分任务完成状态 CAS 失败");
    }
    reconcileSession(task.getExamSessionId(), now);
  }

  @Transactional
  public void fail(CompetitionFormalScoringTaskEntity task, String owner, Throwable failure) {
    LocalDateTime now = LocalDateTime.now();
    int attempts = task.getAttemptCount() == null ? 1 : task.getAttemptCount();
    boolean terminal = attempts >= properties.getMaxAttempts();
    LocalDateTime next = terminal ? now : now.plusSeconds(backoff(attempts));
    String error = abbreviate(failure == null ? null : failure.getMessage(), 1000);
    if (taskMapper.fail(task.getId(), owner, terminal ? "failed" : "retry", next, error, now) != 1) {
      return;
    }
    task.setTaskStatus(terminal ? "failed" : "retry");
    task.setNextAttemptAt(next);
    if (!terminal) {
      enqueueOutbox(task, attempts, next, task.getRequestedBy());
    }
    reconcileSession(task.getExamSessionId(), now);
  }

  @Transactional
  public void recoverAndRepublish(CompetitionFormalScoringTaskEntity task) {
    LocalDateTime now = LocalDateTime.now();
    if ("processing".equals(task.getTaskStatus())
        && taskMapper.recoverExpiredLease(task.getId(), now) != 1) {
      return;
    }
    CompetitionFormalScoringTaskEntity latest = taskMapper.selectById(task.getId());
    if (latest != null && ("pending".equals(latest.getTaskStatus()) || "retry".equals(latest.getTaskStatus()))) {
      // A previously published delivery may have been consumed and failed before the process
      // crashed.  Reusing that deterministic delivery key would find the old published row and
      // silently skip recovery, so every recovery wake-up gets a fresh idempotent key.
      enqueueOutbox(latest, "recovery-" + IdWorker.getId(), now, latest.getRequestedBy());
    }
  }

  @Transactional
  public CompetitionFormalScoringTaskEntity retryFailed(Long taskId, Long operatorId, String reason) {
    CompetitionFormalScoringTaskEntity task = taskMapper.selectById(taskId);
    if (task == null) throw new BusinessException(ErrorCode.NOT_FOUND, "判分任务不存在");
    if ("completed".equals(task.getTaskStatus())) return task;
    if (!"failed".equals(task.getTaskStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅失败任务可以人工重试");
    }
    if (reason == null || reason.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "人工重试必须填写原因");
    }
    LocalDateTime now = LocalDateTime.now();
    task.setTaskStatus("retry"); task.setAttemptCount(0); task.setNextAttemptAt(now);
    task.setLeaseOwner(null); task.setLeaseUntil(null); task.setLastError(null);
    task.setRequestedReason(reason.trim()); task.setRequestedBy(operatorId);
    task.setUpdatedBy(operatorId); task.setUpdatedAt(now);
    taskMapper.updateById(task);
    enqueueOutbox(task, "admin-" + IdWorker.getId(), now, operatorId);
    CompetitionFormalExamSessionEntity session = sessionMapper.selectById(task.getExamSessionId());
    if (session != null) {
      session.setScoringStatus("queued"); session.setScoringError(null);
      session.setUpdatedBy(operatorId); session.setUpdatedAt(now); sessionMapper.updateById(session);
    }
    return task;
  }

  private void enqueueOutbox(CompetitionFormalScoringTaskEntity task, int deliveryNo,
      LocalDateTime availableAt, Long operatorId) {
    enqueueOutbox(task, String.valueOf(deliveryNo), availableAt, operatorId);
  }

  private void enqueueOutbox(CompetitionFormalScoringTaskEntity task, String deliveryKey,
      LocalDateTime availableAt, Long operatorId) {
    String key = "formal-score:" + task.getId() + ":delivery:" + deliveryKey;
    CompetitionOutboxEventEntity existing = outboxMapper.selectOne(
        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CompetitionOutboxEventEntity>()
            .eq(CompetitionOutboxEventEntity::getEventKey, key)
            .eq(CompetitionOutboxEventEntity::getDeleted, 0).last("LIMIT 1"));
    if (existing != null) {
      return;
    }
    CompetitionOutboxEventEntity event = new CompetitionOutboxEventEntity();
    event.setId(IdWorker.getId());
    event.setTenantId(task.getTenantId());
    event.setEventChannel(CHANNEL);
    event.setAggregateType("competition_formal_scoring_task");
    event.setAggregateId(task.getId());
    event.setEventType(EVENT_TYPE);
    event.setRoutingKey(properties.getRoutingKey());
    event.setEventKey(key);
    event.setPayloadJson(serialize(new FormalExamScoringRequestedEvent(task.getTenantId(),
        task.getId(), task.getExamSessionId(), task.getCandidateExamId(), task.getResultRevisionNo())));
    event.setStatus("pending");
    event.setAttemptCount(0);
    event.setNextAttemptAt(availableAt);
    event.setCreatedBy(operatorId);
    event.setUpdatedBy(operatorId);
    event.setCreatedAt(LocalDateTime.now());
    event.setUpdatedAt(LocalDateTime.now());
    event.setDeleted(0);
    if (outboxMapper.insert(event) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "判分 Outbox 创建失败");
    }
  }

  private String serialize(FormalExamScoringRequestedEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "判分事件序列化失败");
    }
  }

  private void reconcileSession(Long sessionId, LocalDateTime now) {
    CompetitionFormalExamSessionEntity session = sessionMapper.selectById(sessionId);
    if (session == null) {
      return;
    }
    List<CompetitionFormalCandidateExamEntity> candidates = candidateMapper.selectByExamSessionId(sessionId);
    int candidateCount = candidates == null ? 0 : candidates.size();
    boolean allClosed = candidates != null && candidates.stream().allMatch(candidate -> candidate != null
        && CompetitionFormalCandidateExamStatus.fromCode(candidate.getStatus()) != null
        && CompetitionFormalCandidateExamStatus.fromCode(candidate.getStatus()).isClosed());
    boolean allTasksPresent = candidateCount > 0 && taskMapper.countBySession(sessionId) == candidateCount;
    if (allClosed && allTasksPresent && taskMapper.countIncompleteBySession(sessionId) == 0) {
      session.setScoringStatus("completed");
      session.setScoredTime(now);
      session.setScoringError(null);
    } else if (taskMapper.countFailedBySession(sessionId) > 0) {
      session.setScoringStatus("failed");
      session.setScoringError("存在判分失败任务，请管理员重试");
    } else if (allTasksPresent) {
      session.setScoringStatus("scoring");
      if (session.getScoringStartedAt() == null) session.setScoringStartedAt(now);
    } else {
      session.setScoringStatus("queued");
    }
    session.setUpdatedAt(now);
    sessionMapper.updateById(session);
  }

  private long backoff(int attempt) {
    return attempt <= 1 ? 1L : attempt == 2 ? 5L : 30L;
  }

  private String abbreviate(String value, int max) {
    String text = value == null || value.isBlank() ? "判分失败" : value.trim();
    return text.length() <= max ? text : text.substring(0, max);
  }
}
