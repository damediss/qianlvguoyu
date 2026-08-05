package com.guoyu.platform.competition.exam.service;

import cn.dev33.satoken.stp.StpLogic;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionTheoryAttemptEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionTokenAuditEntity;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAttemptMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryCandidateAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamAssignmentRow;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionTokenAuditMapper;
import com.guoyu.platform.competition.service.CompetitionContestantScopeValidator;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionTheoryExamExecutionService {
  private final CompetitionTheoryExamSessionMapper sessionMapper;
  private final CompetitionTheoryCandidateAssignmentMapper assignmentMapper;
  private final CompetitionTheoryAttemptMapper attemptMapper;
  private final CompetitionTheoryExamSessionTokenAuditMapper auditMapper;
  private final CompetitionTheorySubmissionService submissionService;
  private final CompetitionContestantScopeValidator contestantScopeValidator;
  private final StpLogic examStpLogic;
  private final CompetitionTheoryCandidateExecutionService candidateExecutionService;
  private final CompetitionTheorySessionLifecycleService sessionLifecycleService;

  public CompetitionTheoryExamExecutionService(
      CompetitionTheoryExamSessionMapper sessionMapper,
      CompetitionTheoryCandidateAssignmentMapper assignmentMapper,
      CompetitionTheoryAttemptMapper attemptMapper,
      CompetitionTheoryExamSessionTokenAuditMapper auditMapper,
      CompetitionTheorySubmissionService submissionService,
      CompetitionContestantScopeValidator contestantScopeValidator,
      @Qualifier("competitionExamStpLogic") StpLogic examStpLogic) {
    this(sessionMapper, assignmentMapper, attemptMapper, auditMapper, submissionService,
        contestantScopeValidator, examStpLogic, null, null);
  }

  @org.springframework.beans.factory.annotation.Autowired
  public CompetitionTheoryExamExecutionService(
      CompetitionTheoryExamSessionMapper sessionMapper,
      CompetitionTheoryCandidateAssignmentMapper assignmentMapper,
      CompetitionTheoryAttemptMapper attemptMapper,
      CompetitionTheoryExamSessionTokenAuditMapper auditMapper,
      CompetitionTheorySubmissionService submissionService,
      CompetitionContestantScopeValidator contestantScopeValidator,
      @Qualifier("competitionExamStpLogic") StpLogic examStpLogic,
      CompetitionTheoryCandidateExecutionService candidateExecutionService,
      CompetitionTheorySessionLifecycleService sessionLifecycleService) {
    this.sessionMapper = sessionMapper;
    this.assignmentMapper = assignmentMapper;
    this.attemptMapper = attemptMapper;
    this.auditMapper = auditMapper;
    this.submissionService = submissionService;
    this.contestantScopeValidator = contestantScopeValidator;
    this.examStpLogic = examStpLogic;
    this.candidateExecutionService = candidateExecutionService;
    this.sessionLifecycleService = sessionLifecycleService;
  }

  public List<Long> executionTenantIds(LocalDateTime now) {
    List<Long> tenantIds = sessionMapper.selectExecutionTenantIds(now);
    return tenantIds == null ? List.of() : tenantIds;
  }

  public List<CompetitionTheoryExamSessionEntity> sessionsForExecution(LocalDateTime now) {
    List<CompetitionTheoryExamSessionEntity> sessions = sessionMapper.selectSessionsForExecution(now);
    return sessions == null ? List.of() : sessions;
  }

  public void processSession(Long tenantId, Long sessionId, LocalDateTime now) {
    CompetitionTheoryExamSessionEntity session = sessionMapper.selectById(sessionId);
    if (session == null || !tenantId.equals(session.getTenantId())
        || !"published".equals(session.getStatus())) {
      return;
    }
    List<CompetitionTheoryExamAssignmentRow> assignments =
        assignmentMapper.selectPublishedExamAssignmentsBySession(tenantId, sessionId);
    if (assignments == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试场次分配查询失败");
    }
    if (candidateExecutionService != null) {
      if (!now.isBefore(session.getStartAt())) {
        if (sessionLifecycleService != null) {
          sessionLifecycleService.openIfDue(sessionId, now);
        } else {
          sessionMapper.openIfDue(sessionId, now);
        }
      }
      for (CompetitionTheoryExamAssignmentRow assignment : assignments) {
        try {
          CompetitionTheoryCandidateExecutionService.ExecutionResult result =
              candidateExecutionService.processAssignment(assignment, session.getEndAt(), now);
          if (result.shouldLogout() && result.playerId() != null) {
            // Token cleanup is deliberately outside the database transaction.
            examStpLogic.logout(result.playerId());
          }
        } catch (BusinessException ex) {
          if (ErrorCode.UNPROCESSABLE_ENTITY.equals(ex.getCode())) {
            log.warn("Skipping moved contestant during automatic execution, tenantId={}, assignmentId={}, message={}",
                tenantId, assignment.getAssignmentId(), ex.getMessage());
            continue;
          }
          throw ex;
        }
      }
      if (!now.isBefore(session.getEndAt())) {
        if (sessionLifecycleService != null) {
          sessionLifecycleService.closeIfDue(sessionId, now);
        } else {
          sessionMapper.closeIfDue(sessionId, now);
        }
      }
      return;
    }
    for (CompetitionTheoryExamAssignmentRow assignment : assignments) {
      contestantScopeValidator.validate(
          tenantId,
          assignment.getEventId(),
          assignment.getStageId(),
          assignment.getEventWorkTypeId(),
          assignment.getContestantMemberId());
    }
    if (!now.isBefore(session.getStartAt())) {
      sessionMapper.openIfDue(sessionId, now);
    }
    for (CompetitionTheoryExamAssignmentRow assignment : assignments) {
      CompetitionTheoryAttemptEntity attempt = ensureAttempt(assignment, now);
      if (!now.isBefore(session.getEndAt())) {
        if ("in_progress".equals(attempt.getStatus())) {
          submissionService.submit(
              attempt.getId(), "auto", "auto:" + attempt.getId(), null, null, now);
        } else if (!isTerminal(attempt.getStatus())) {
          submissionService.finalizeAbsence(attempt.getId(), now);
        }
        examStpLogic.logout(attempt.getPlayerId());
      } else if (!now.isBefore(session.getStartAt().plusMinutes(30))
          && ("waiting".equals(attempt.getStatus()) || "ready".equals(attempt.getStatus()))) {
        if (attemptMapper.markProvisionalAbsent(attempt.getId(), now) == 1) {
          writeAudit(attempt, "provisional_absent", "closed", now);
          examStpLogic.logout(attempt.getPlayerId());
        }
      }
    }
    if (!now.isBefore(session.getEndAt())) {
      sessionMapper.closeIfDue(sessionId, now);
    }
  }

  private CompetitionTheoryAttemptEntity ensureAttempt(
      CompetitionTheoryExamAssignmentRow assignment, LocalDateTime now) {
    CompetitionTheoryAttemptEntity existing =
        attemptMapper.selectForUpdateByAssignment(assignment.getAssignmentId());
    if (existing != null) {
      return existing;
    }
    CompetitionTheoryAttemptEntity attempt = new CompetitionTheoryAttemptEntity();
    attempt.setTenantId(assignment.getTenantId());
    attempt.setAssignmentId(assignment.getAssignmentId());
    attempt.setScheduleVersionId(assignment.getScheduleVersionId());
    attempt.setSessionId(assignment.getSessionId());
    attempt.setPaperSnapshotId(assignment.getPaperSnapshotId());
    attempt.setEventId(assignment.getEventId());
    attempt.setStageId(assignment.getStageId());
    attempt.setEventWorkTypeId(assignment.getEventWorkTypeId());
    attempt.setContestantProfileId(assignment.getContestantProfileId());
    attempt.setContestantMemberId(assignment.getContestantMemberId());
    attempt.setPlayerId(assignment.getPlayerId());
    attempt.setStatus(now.isBefore(assignment.getStartAt()) ? "waiting" : "ready");
    attempt.setSessionState("offline");
    attempt.setAbsenceFlag(0);
    attempt.setRestoreCount(0);
    attempt.setLockVersion(0);
    attempt.setDeleted(0);
    if (attemptMapper.insert(attempt) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试作答记录创建失败");
    }
    return attempt;
  }

  private boolean isTerminal(String status) {
    return status != null && (status.startsWith("submitted_") || "absent".equals(status));
  }

  private void writeAudit(
      CompetitionTheoryAttemptEntity attempt,
      String eventType,
      String sessionStatus,
      LocalDateTime now) {
    CompetitionTheoryExamSessionTokenAuditEntity audit =
        new CompetitionTheoryExamSessionTokenAuditEntity();
    audit.setTenantId(attempt.getTenantId());
    audit.setPlayerId(attempt.getPlayerId());
    audit.setAssignmentId(attempt.getAssignmentId());
    audit.setAttemptId(attempt.getId());
    audit.setEventType(eventType);
    audit.setSessionStatus(sessionStatus);
    audit.setOccurredAt(now);
    audit.setOperatedBy(0L);
    audit.setReason("开考30分钟仍未开始");
    audit.setDeleted(0);
    if (auditMapper.insert(audit) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试缺考审计保存失败");
    }
  }
}
