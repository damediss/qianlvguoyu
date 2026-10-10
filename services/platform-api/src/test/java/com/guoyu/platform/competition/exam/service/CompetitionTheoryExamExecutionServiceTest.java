package com.guoyu.platform.competition.exam.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompetitionTheoryExamExecutionServiceTest {
  private CompetitionTheoryExamSessionMapper sessionMapper;
  private CompetitionTheoryCandidateAssignmentMapper assignmentMapper;
  private CompetitionTheoryAttemptMapper attemptMapper;
  private CompetitionTheoryExamSessionTokenAuditMapper auditMapper;
  private CompetitionTheorySubmissionService submissionService;
  private CompetitionContestantScopeValidator contestantScopeValidator;
  private StpLogic stpLogic;
  private CompetitionTheoryExamExecutionService service;

  @BeforeEach
  void setUp() {
    sessionMapper = mock(CompetitionTheoryExamSessionMapper.class);
    assignmentMapper = mock(CompetitionTheoryCandidateAssignmentMapper.class);
    attemptMapper = mock(CompetitionTheoryAttemptMapper.class);
    auditMapper = mock(CompetitionTheoryExamSessionTokenAuditMapper.class);
    submissionService = mock(CompetitionTheorySubmissionService.class);
    contestantScopeValidator = mock(CompetitionContestantScopeValidator.class);
    stpLogic = mock(StpLogic.class);
    service = new CompetitionTheoryExamExecutionService(
        sessionMapper, assignmentMapper, attemptMapper, auditMapper, submissionService,
        contestantScopeValidator, stpLogic);
  }

  @Test
  void marksProvisionalAbsenceIdempotentlyThirtyMinutesAfterStart() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 31, 10, 31);
    CompetitionTheoryExamSessionEntity session = session(1L, now.minusMinutes(31), now.plusMinutes(29));
    CompetitionTheoryExamAssignmentRow assignment = assignment(7L);
    CompetitionTheoryAttemptEntity attempt = attempt(70L, "waiting");
    when(sessionMapper.selectById(9L)).thenReturn(session);
    when(assignmentMapper.selectPublishedExamAssignmentsBySession(1L, 9L))
        .thenReturn(List.of(assignment));
    when(attemptMapper.selectForUpdateByAssignment(7L)).thenReturn(attempt);
    when(attemptMapper.markProvisionalAbsent(70L, now)).thenReturn(1);
    when(auditMapper.insert(any(CompetitionTheoryExamSessionTokenAuditEntity.class))).thenReturn(1);

    service.processSession(1L, 9L, now);

    verify(attemptMapper, never()).insert(any(CompetitionTheoryAttemptEntity.class));
    verify(attemptMapper).markProvisionalAbsent(70L, now);
    verify(auditMapper).insert(any(CompetitionTheoryExamSessionTokenAuditEntity.class));
    verify(stpLogic).logout(700L);
  }

  @Test
  void delegatesEndTimeSubmissionToSingleClaimedSubmissionPath() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 31, 11, 0);
    CompetitionTheoryExamSessionEntity session = session(1L, now.minusHours(1), now);
    CompetitionTheoryExamAssignmentRow assignment = assignment(8L);
    CompetitionTheoryAttemptEntity attempt = attempt(80L, "in_progress");
    when(sessionMapper.selectById(9L)).thenReturn(session);
    when(assignmentMapper.selectPublishedExamAssignmentsBySession(1L, 9L))
        .thenReturn(List.of(assignment));
    when(attemptMapper.selectForUpdateByAssignment(8L)).thenReturn(attempt);

    service.processSession(1L, 9L, now);

    verify(submissionService).submit(80L, "auto", "auto:80", null, null, now);
    verify(sessionMapper).closeIfDue(9L, now);
    verify(stpLogic).logout(700L);
  }

  @Test
  void rejectsMovedContestantBeforeOpeningSessionOrCreatingAttempt() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 31, 11, 0);
    CompetitionTheoryExamSessionEntity session = session(1L, now.minusHours(1), now);
    CompetitionTheoryExamAssignmentRow assignment = assignment(8L);
    assignment.setEventId(100L);
    assignment.setStageId(200L);
    assignment.setEventWorkTypeId(300L);
    assignment.setContestantMemberId(400L);
    when(sessionMapper.selectById(9L)).thenReturn(session);
    when(assignmentMapper.selectPublishedExamAssignmentsBySession(1L, 9L))
        .thenReturn(List.of(assignment));
    doThrow(new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY,
        "选手已归属其他赛段，不能处理当前赛段成绩"))
        .when(contestantScopeValidator)
        .validate(1L, 100L, 200L, 300L, 400L);

    org.assertj.core.api.Assertions.assertThatThrownBy(
        () -> service.processSession(1L, 9L, now))
        .isInstanceOf(BusinessException.class);

    verify(sessionMapper, never()).openIfDue(9L, now);
    verify(sessionMapper, never()).closeIfDue(9L, now);
    verify(attemptMapper, never()).selectForUpdateByAssignment(8L);
    verify(attemptMapper, never()).insert(any(CompetitionTheoryAttemptEntity.class));
    verify(submissionService, never()).submit(any(), any(), any(), any(), any(), any());
  }

  private CompetitionTheoryExamSessionEntity session(
      Long tenantId, LocalDateTime startAt, LocalDateTime endAt) {
    CompetitionTheoryExamSessionEntity session = new CompetitionTheoryExamSessionEntity();
    session.setTenantId(tenantId);
    session.setStatus("published");
    session.setStartAt(startAt);
    session.setEndAt(endAt);
    return session;
  }

  private CompetitionTheoryExamAssignmentRow assignment(Long assignmentId) {
    CompetitionTheoryExamAssignmentRow row = new CompetitionTheoryExamAssignmentRow();
    row.setAssignmentId(assignmentId);
    return row;
  }

  private CompetitionTheoryAttemptEntity attempt(Long id, String status) {
    CompetitionTheoryAttemptEntity attempt = new CompetitionTheoryAttemptEntity();
    attempt.setId(id);
    attempt.setTenantId(1L);
    attempt.setAssignmentId(id / 10);
    attempt.setPlayerId(700L);
    attempt.setStatus(status);
    return attempt;
  }
}
