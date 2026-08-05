package com.guoyu.platform.competition.exam.service;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAttemptEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionTokenAuditEntity;
import com.guoyu.platform.competition.exam.vo.CompetitionTheoryMonitorVO;
import com.guoyu.platform.competition.exam.vo.CompetitionTheoryResultSummaryVO;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAttemptMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryCandidateAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionTokenAuditMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryRosterRow;
import com.guoyu.platform.competition.service.CompetitionTheoryScheduleApplicationService;
import com.guoyu.platform.competition.service.CompetitionContestantScopeValidator;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
public class CompetitionTheoryMonitorApplicationService {
  private final CompetitionTheoryScheduleApplicationService scheduleService;
  private final CompetitionTheoryCandidateAssignmentMapper assignmentMapper;
  private final CompetitionTheoryAttemptMapper attemptMapper;
  private final CompetitionTheoryExamSessionMapper sessionMapper;
  private final CompetitionScoreSourceMapper scoreSourceMapper;
  private final CompetitionTheoryExamSessionTokenAuditMapper auditMapper;
  private final CompetitionTheorySubmissionService submissionService;
  private final StpLogic examStpLogic;
  private final Clock clock;
  private final CompetitionContestantScopeValidator scopeValidator;

  public CompetitionTheoryMonitorApplicationService(
      CompetitionTheoryScheduleApplicationService scheduleService,
      CompetitionTheoryCandidateAssignmentMapper assignmentMapper,
      CompetitionTheoryAttemptMapper attemptMapper,
      CompetitionTheoryExamSessionMapper sessionMapper,
      CompetitionScoreSourceMapper scoreSourceMapper,
      CompetitionTheoryExamSessionTokenAuditMapper auditMapper,
      CompetitionTheorySubmissionService submissionService,
      @Qualifier("competitionExamStpLogic") StpLogic examStpLogic,
      Clock clock) {
    this(scheduleService, assignmentMapper, attemptMapper, sessionMapper, scoreSourceMapper,
        auditMapper, submissionService, examStpLogic, clock, null);
  }

  @org.springframework.beans.factory.annotation.Autowired
  public CompetitionTheoryMonitorApplicationService(
      CompetitionTheoryScheduleApplicationService scheduleService,
      CompetitionTheoryCandidateAssignmentMapper assignmentMapper,
      CompetitionTheoryAttemptMapper attemptMapper,
      CompetitionTheoryExamSessionMapper sessionMapper,
      CompetitionScoreSourceMapper scoreSourceMapper,
      CompetitionTheoryExamSessionTokenAuditMapper auditMapper,
      CompetitionTheorySubmissionService submissionService,
      @Qualifier("competitionExamStpLogic") StpLogic examStpLogic,
      Clock clock,
      CompetitionContestantScopeValidator scopeValidator) {
    this.scheduleService = scheduleService;
    this.assignmentMapper = assignmentMapper;
    this.attemptMapper = attemptMapper;
    this.sessionMapper = sessionMapper;
    this.scoreSourceMapper = scoreSourceMapper;
    this.auditMapper = auditMapper;
    this.submissionService = submissionService;
    this.examStpLogic = examStpLogic;
    this.clock = clock;
    this.scopeValidator = scopeValidator;
  }

  @Transactional(readOnly = true)
  public List<CompetitionTheoryMonitorVO> monitor(Long scheduleVersionId) {
    scheduleService.detail(scheduleVersionId);
    List<CompetitionTheoryRosterRow> roster =
        assignmentMapper.selectRosterRows(currentUser().getTenantId(), scheduleVersionId);
    if (roster == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "理论考试名单查询失败");
    }
    List<CompetitionTheoryAttemptEntity> attempts = attemptMapper.selectList(
        new LambdaQueryWrapper<CompetitionTheoryAttemptEntity>()
            .eq(CompetitionTheoryAttemptEntity::getScheduleVersionId, scheduleVersionId)
            .eq(CompetitionTheoryAttemptEntity::getDeleted, 0));
    Map<Long, CompetitionTheoryAttemptEntity> byAssignment = new HashMap<>();
    if (attempts != null) {
      attempts.forEach(attempt -> byAssignment.put(attempt.getAssignmentId(), attempt));
    }
    List<CompetitionTheoryMonitorVO> result = new ArrayList<>(roster.size());
    for (CompetitionTheoryRosterRow row : roster) {
      CompetitionTheoryAttemptEntity attempt = byAssignment.get(row.getAssignmentId());
      result.add(new CompetitionTheoryMonitorVO(
          attempt == null ? null : attempt.getId(), row.getAssignmentId(), row.getPlayerId(),
          row.getContestantNo(), row.getContestantName(), row.getOrgName(), row.getSessionName(),
          row.getRoomName(), row.getSeatNo(), attempt == null ? "not_logged_in" : displayStatus(attempt),
          attempt == null ? null : attempt.getStartedAt(),
          attempt == null ? null : attempt.getLastSavedAt(),
          attempt == null ? null : attempt.getSubmittedAt(),
          row.getPlayerId() != null && examStpLogic.isLogin(row.getPlayerId())));
    }
    return result;
  }

  @Transactional
  public void forceOffline(Long attemptId, String reason) {
    CompetitionTheoryAttemptEntity attempt = requireAuthorizedAttempt(
        attemptId, true, CompetitionContestantScopeValidator.UpdateIntent.CLEANUP_ONLY);
    attemptMapper.updateSessionState(attempt.getId(), "forced_offline", LocalDateTime.now(clock));
    writeAudit(attempt, "force_offline", "forced_offline", reason, currentUser().getUserId());
    afterCommitTokenAction(() -> examStpLogic.kickout(attempt.getPlayerId()));
  }

  public void forceSubmit(Long attemptId, String reason) {
    CompetitionTheoryAttemptEntity attempt = requireAuthorizedAttempt(
        attemptId, false, CompetitionContestantScopeValidator.UpdateIntent.PARTICIPATION);
    if (!"in_progress".equals(attempt.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅答题中的选手可以强制交卷");
    }
    PlatformUserPrincipal user = currentUser();
    submissionService.submit(
        attemptId, "forced", "forced:" + UUID.randomUUID(), user.getUserId(), reason,
        LocalDateTime.now(clock));
    examStpLogic.kickout(attempt.getPlayerId());
  }

  @Transactional
  public void restoreAbsence(Long attemptId, String reason) {
    CompetitionTheoryAttemptEntity attempt = requireAuthorizedAttempt(
        attemptId, true, CompetitionContestantScopeValidator.UpdateIntent.PARTICIPATION);
    if (!"provisional_absent".equals(attempt.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "仅暂定缺考状态可以恢复");
    }
    CompetitionTheoryExamSessionEntity session = sessionMapper.selectById(attempt.getSessionId());
    LocalDateTime now = LocalDateTime.now(clock);
    if (session == null || !now.isBefore(session.getEndAt())) {
      throw new BusinessException(ErrorCode.CONFLICT, "场次结束后不能恢复缺考");
    }
    if (attemptMapper.restoreAbsence(attemptId, now) != 1) {
      throw new BusinessException(ErrorCode.CONFLICT, "缺考状态已发生变化");
    }
    writeAudit(attempt, "restore_absence", "closed", reason, currentUser().getUserId());
  }

  @Transactional(readOnly = true)
  public List<CompetitionTheoryResultSummaryVO> results(Long scheduleVersionId) {
    scheduleService.detail(scheduleVersionId);
    List<CompetitionTheoryRosterRow> roster =
        assignmentMapper.selectRosterRows(currentUser().getTenantId(), scheduleVersionId);
    List<CompetitionTheoryAttemptEntity> attempts = attemptMapper.selectList(
        new LambdaQueryWrapper<CompetitionTheoryAttemptEntity>()
            .eq(CompetitionTheoryAttemptEntity::getScheduleVersionId, scheduleVersionId)
            .eq(CompetitionTheoryAttemptEntity::getDeleted, 0));
    if (roster == null || attempts == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "理论考试成绩摘要查询失败");
    }
    Map<Long, CompetitionTheoryRosterRow> rosterByAssignment = new HashMap<>();
    roster.forEach(row -> rosterByAssignment.put(row.getAssignmentId(), row));
    List<CompetitionTheoryResultSummaryVO> result = new ArrayList<>();
    for (CompetitionTheoryAttemptEntity attempt : attempts) {
      if (attempt.getScoreSourceId() == null) {
        continue;
      }
      CompetitionTheoryRosterRow row = rosterByAssignment.get(attempt.getAssignmentId());
      CompetitionScoreSourceEntity source = scoreSourceMapper.selectById(attempt.getScoreSourceId());
      if (row == null || source == null) {
        continue;
      }
      result.add(new CompetitionTheoryResultSummaryVO(
          attempt.getId(), attempt.getPlayerId(), row.getContestantNo(), row.getContestantName(),
          attempt.getStatus(), Integer.valueOf(1).equals(attempt.getAbsenceFlag()),
          attempt.getTotalScore(), source.getConfirmationStatus(), false, attempt.getSubmittedAt()));
    }
    return result;
  }

  private CompetitionTheoryAttemptEntity requireAuthorizedAttempt(
      Long attemptId, boolean lock, CompetitionContestantScopeValidator.UpdateIntent intent) {
    CompetitionTheoryAttemptEntity attempt = attemptMapper.selectById(attemptId);
    if (attempt == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "考试作答记录不存在");
    }
    if (!currentUser().getTenantId().equals(attempt.getTenantId())) {
      log.warn("Competition theory monitor cross-tenant access rejected, attemptId={}", attemptId);
      throw new BusinessException(ErrorCode.FORBIDDEN, "考试作答范围不匹配");
    }
    scheduleService.detail(attempt.getScheduleVersionId());
    if (scopeValidator != null) {
      scopeValidator.validateForUpdate(
          attempt.getTenantId(), attempt.getEventId(), attempt.getStageId(),
          attempt.getEventWorkTypeId(), attempt.getContestantMemberId(), intent);
    }
    if (!lock) {
      return attempt;
    }
    CompetitionTheoryAttemptEntity locked = attemptMapper.selectForUpdateById(attemptId);
    if (locked == null || !attempt.getTenantId().equals(locked.getTenantId())
        || !attempt.getContestantMemberId().equals(locked.getContestantMemberId())) {
      throw new BusinessException(ErrorCode.CONFLICT, "考试范围已发生变化，请重试");
    }
    return locked;
  }

  private void afterCommitTokenAction(Runnable action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        action.run();
      }
    });
  }

  private String displayStatus(CompetitionTheoryAttemptEntity attempt) {
    if ("in_progress".equals(attempt.getStatus())) {
      if ("forced_offline".equals(attempt.getSessionState())) {
        return "forced_offline";
      }
      if (!"active".equals(attempt.getSessionState())) {
        return "abnormal_offline";
      }
    }
    return attempt.getStatus();
  }

  private PlatformUserPrincipal currentUser() {
    try {
      return SecurityContext.currentUser();
    } catch (IllegalStateException ex) {
      log.warn("Competition theory monitor request has no platform principal");
      throw ex;
    }
  }

  private void writeAudit(
      CompetitionTheoryAttemptEntity attempt,
      String eventType,
      String status,
      String reason,
      Long operatedBy) {
    CompetitionTheoryExamSessionTokenAuditEntity audit =
        new CompetitionTheoryExamSessionTokenAuditEntity();
    audit.setTenantId(attempt.getTenantId());
    audit.setPlayerId(attempt.getPlayerId());
    audit.setAssignmentId(attempt.getAssignmentId());
    audit.setAttemptId(attempt.getId());
    audit.setEventType(eventType);
    audit.setSessionStatus(status);
    audit.setReason(reason);
    audit.setOperatedBy(operatedBy);
    audit.setOccurredAt(LocalDateTime.now(clock));
    audit.setDeleted(0);
    if (auditMapper.insert(audit) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试监控操作审计保存失败");
    }
  }
}
