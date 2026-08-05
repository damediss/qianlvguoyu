package com.guoyu.platform.competition.exam.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionTheoryAttemptEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionTokenAuditEntity;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAttemptMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryCandidateAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamAssignmentRow;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionTokenAuditMapper;
import com.guoyu.platform.competition.service.CompetitionContestantScopeValidator;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** One short profile-first transaction for one scheduled contestant assignment. */
@Service
@Slf4j
public class CompetitionTheoryCandidateExecutionService {

  private final CompetitionTheoryAttemptMapper attemptMapper;
  private final CompetitionTheoryCandidateAssignmentMapper assignmentMapper;
  private final CompetitionTheoryExamSessionTokenAuditMapper auditMapper;
  private final CompetitionTheorySubmissionService submissionService;
  private final CompetitionContestantScopeValidator scopeValidator;

  public CompetitionTheoryCandidateExecutionService(
      CompetitionTheoryAttemptMapper attemptMapper,
      CompetitionTheoryCandidateAssignmentMapper assignmentMapper,
      CompetitionTheoryExamSessionTokenAuditMapper auditMapper,
      CompetitionTheorySubmissionService submissionService,
      CompetitionContestantScopeValidator scopeValidator) {
    this.attemptMapper = attemptMapper;
    this.assignmentMapper = assignmentMapper;
    this.auditMapper = auditMapper;
    this.submissionService = submissionService;
    this.scopeValidator = scopeValidator;
  }

  @Transactional
  public ExecutionResult processAssignment(
      CompetitionTheoryExamAssignmentRow assignment, LocalDateTime sessionEnd, LocalDateTime now) {
    scopeValidator.validateForUpdate(
        assignment.getTenantId(), assignment.getEventId(), assignment.getStageId(),
        assignment.getEventWorkTypeId(), assignment.getContestantMemberId());
    CompetitionTheoryAttemptEntity attempt = attemptMapper.selectForUpdateByAssignment(
        assignment.getAssignmentId());
    if (attempt == null) {
      attempt = createAttempt(assignment, now);
    }
    if (!now.isBefore(sessionEnd)) {
      if ("in_progress".equals(attempt.getStatus())) {
        submissionService.submit(
            attempt.getId(), "auto", "auto:" + attempt.getId(), null, null, now);
      } else if (!isTerminal(attempt.getStatus())) {
        submissionService.finalizeAbsence(attempt.getId(), now);
      }
      return new ExecutionResult(attempt.getPlayerId(), true);
    }
    if (!now.isBefore(assignment.getStartAt().plusMinutes(30))
        && ("waiting".equals(attempt.getStatus()) || "ready".equals(attempt.getStatus()))
        && attemptMapper.markProvisionalAbsent(attempt.getId(), now) == 1) {
      writeAudit(attempt, "provisional_absent", "closed", now);
      return new ExecutionResult(attempt.getPlayerId(), true);
    }
    return new ExecutionResult(attempt.getPlayerId(), false);
  }

  private CompetitionTheoryAttemptEntity createAttempt(
      CompetitionTheoryExamAssignmentRow assignment, LocalDateTime now) {
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

  private void writeAudit(
      CompetitionTheoryAttemptEntity attempt, String eventType, String status, LocalDateTime now) {
    CompetitionTheoryExamSessionTokenAuditEntity audit =
        new CompetitionTheoryExamSessionTokenAuditEntity();
    audit.setTenantId(attempt.getTenantId());
    audit.setPlayerId(attempt.getPlayerId());
    audit.setAssignmentId(attempt.getAssignmentId());
    audit.setAttemptId(attempt.getId());
    audit.setEventType(eventType);
    audit.setSessionStatus(status);
    audit.setOccurredAt(now);
    audit.setOperatedBy(0L);
    audit.setReason("开考30分钟仍未开始");
    audit.setDeleted(0);
    if (auditMapper.insert(audit) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "考试缺考审计保存失败");
    }
  }

  private boolean isTerminal(String status) {
    return status != null && (status.startsWith("submitted_") || "absent".equals(status));
  }

  public record ExecutionResult(Long playerId, boolean shouldLogout) {
  }
}
