package com.guoyu.platform.competition.exam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryAttemptEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionTheoryPaperSnapshotEntity;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryAttemptMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryExamSessionTokenAuditMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.service.CompetitionContestantScopeValidator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CompetitionTheorySubmissionServiceTest {
  private static final Long STAGE_ID = 200L;
  private final CompetitionTheoryAttemptMapper attemptMapper = Mockito.mock(CompetitionTheoryAttemptMapper.class);
  private final CompetitionTheoryAnswerMapper answerMapper = Mockito.mock(CompetitionTheoryAnswerMapper.class);
  private final CompetitionTheoryPaperQuestionMapper questionMapper =
      Mockito.mock(CompetitionTheoryPaperQuestionMapper.class);
  private final CompetitionTheoryPaperSnapshotMapper paperMapper =
      Mockito.mock(CompetitionTheoryPaperSnapshotMapper.class);
  private final CompetitionTheoryExamSessionMapper sessionMapper =
      Mockito.mock(CompetitionTheoryExamSessionMapper.class);
  private final CompetitionScoreSourceMapper sourceMapper = Mockito.mock(CompetitionScoreSourceMapper.class);
  private final CompetitionTheoryExamSessionTokenAuditMapper auditMapper =
      Mockito.mock(CompetitionTheoryExamSessionTokenAuditMapper.class);
  private final CompetitionContestantScopeValidator contestantScopeValidator =
      Mockito.mock(CompetitionContestantScopeValidator.class);
  private final CompetitionTheorySubmissionService service = new CompetitionTheorySubmissionService(
      attemptMapper, answerMapper, questionMapper, paperMapper, sessionMapper, sourceMapper,
      auditMapper, contestantScopeValidator, new ObjectMapper());

  @Test
  void manualSubmissionBeforeThirtyMinutesIsRejected() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 9, 10);
    CompetitionTheoryAttemptEntity attempt = attempt("in_progress");
    CompetitionTheoryExamSessionEntity session = session(now.minusMinutes(10), now.plusHours(1));
    when(attemptMapper.selectForUpdateById(1L)).thenReturn(attempt);
    when(sessionMapper.selectById(20L)).thenReturn(session);

    assertThatThrownBy(() -> service.submit(1L, "manual", "req-1", null, null, now))
        .isInstanceOf(BusinessException.class)
        .hasMessage("开考满30分钟后方可主动交卷");
    verify(attemptMapper, never()).claimSubmission(any(), any(), any(), any(), any(), any());
  }

  @Test
  void concurrentLoserReturnsTheSingleCommittedTerminalResult() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 10, 0);
    CompetitionTheoryAttemptEntity active = attempt("in_progress");
    CompetitionTheoryAttemptEntity terminal = attempt("submitted_auto");
    when(attemptMapper.selectForUpdateById(1L)).thenReturn(active, terminal);
    when(sessionMapper.selectById(20L)).thenReturn(session(now.minusHours(1), now));
    when(attemptMapper.claimSubmission(1L, "auto", "auto:1", null, null, now)).thenReturn(0);

    CompetitionTheoryAttemptEntity result = service.submit(1L, "auto", "auto:1", null, null, now);

    assertThat(result.getStatus()).isEqualTo("submitted_auto");
    verify(sourceMapper, never()).insert(any(CompetitionScoreSourceEntity.class));
  }

  @Test
  void objectiveScoringCreatesOneConfirmedUnpublishedSourceAndFinalizesAttempt() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 10, 0);
    CompetitionTheoryAttemptEntity active = attempt("in_progress");
    CompetitionTheoryAttemptEntity terminal = attempt("submitted_manual");
    when(attemptMapper.selectForUpdateById(1L)).thenReturn(active, terminal);
    when(sessionMapper.selectById(20L)).thenReturn(session(now.minusHours(1), now.plusHours(1)));
    when(attemptMapper.claimSubmission(1L, "manual", "req-2", null, null, now)).thenReturn(1);
    CompetitionTheoryPaperQuestionEntity single = question(101L, "single", "[\"A\"]");
    CompetitionTheoryPaperQuestionEntity multi = question(102L, "multi", "[\"A\",\"B\"]");
    when(questionMapper.selectByPaper(30L)).thenReturn(List.of(single, multi));
    when(answerMapper.selectByAttempt(1L)).thenReturn(List.of(
        answer(101L, "[\"A\"]"), answer(102L, "[\"B\",\"A\"]")));
    when(answerMapper.updateById(any(CompetitionTheoryAnswerEntity.class))).thenReturn(1);
    CompetitionTheoryPaperSnapshotEntity paper = new CompetitionTheoryPaperSnapshotEntity();
    paper.setId(30L);
    paper.setScoreRuleId(40L);
    when(paperMapper.selectById(30L)).thenReturn(paper);
    when(sourceMapper.selectList(any())).thenReturn(List.of());
    when(sourceMapper.insert(any(CompetitionScoreSourceEntity.class))).thenAnswer(invocation -> {
      CompetitionScoreSourceEntity source = invocation.getArgument(0);
      source.setId(50L);
      return 1;
    });
    when(attemptMapper.finishSubmission(eq(1L), eq("submitted_manual"), any(), eq(50L), eq(now)))
        .thenReturn(1);
    when(auditMapper.insert(any(
        com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionTokenAuditEntity.class)))
        .thenReturn(1);

    CompetitionTheoryAttemptEntity result = service.submit(1L, "manual", "req-2", null, null, now);

    assertThat(result.getStatus()).isEqualTo("submitted_manual");
    ArgumentCaptor<BigDecimal> total = ArgumentCaptor.forClass(BigDecimal.class);
    verify(attemptMapper).finishSubmission(
        eq(1L), eq("submitted_manual"), total.capture(), eq(50L), eq(now));
    assertThat(total.getValue()).isEqualByComparingTo("100");
    ArgumentCaptor<CompetitionScoreSourceEntity> source =
        ArgumentCaptor.forClass(CompetitionScoreSourceEntity.class);
    verify(sourceMapper).insert(source.capture());
    assertThat(source.getValue().getConfirmationStatus()).isEqualTo("confirmed");
    assertThat(source.getValue().getSourceSystem()).isEqualTo("competition-theory-exam");
    assertThat(source.getValue().getSourceReference()).isEqualTo("theory-attempt:1");
  }

  @Test
  void manualSubmissionAfterContestantMovedToAnotherStageIsRejectedBeforeAnyWrite() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 10, 0);
    when(attemptMapper.selectForUpdateById(1L)).thenReturn(attempt("in_progress"));
    rejectAttemptScope();

    assertThatThrownBy(() -> service.submit(1L, "manual", "req-moved-manual", null, null, now))
        .isInstanceOf(BusinessException.class)
        .hasMessage("选手已归属其他赛段，不能处理当前赛段成绩");

    verifyNoInteractions(sessionMapper, answerMapper, questionMapper, paperMapper, auditMapper);
    verify(attemptMapper, never()).claimSubmission(any(), any(), any(), any(), any(), any());
    verify(attemptMapper, never()).finishSubmission(any(), any(), any(), any(), any());
    verify(sourceMapper, never()).selectList(any());
    verify(sourceMapper, never()).insert(any(CompetitionScoreSourceEntity.class));
  }

  @Test
  void autoSubmissionAfterContestantMovedToAnotherStageIsRejectedBeforeAnyWrite() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 10, 0);
    when(attemptMapper.selectForUpdateById(1L)).thenReturn(attempt("in_progress"));
    rejectAttemptScope();

    assertThatThrownBy(() -> service.submit(1L, "auto", "auto:1", null, null, now))
        .isInstanceOf(BusinessException.class)
        .hasMessage("选手已归属其他赛段，不能处理当前赛段成绩");

    verifyNoInteractions(sessionMapper, answerMapper, questionMapper, paperMapper, auditMapper);
    verify(attemptMapper, never()).claimSubmission(any(), any(), any(), any(), any(), any());
    verify(attemptMapper, never()).finishSubmission(any(), any(), any(), any(), any());
    verify(sourceMapper, never()).selectList(any());
    verify(sourceMapper, never()).insert(any(CompetitionScoreSourceEntity.class));
  }

  @Test
  void absenceFinalizationAfterContestantMovedToAnotherStageIsRejectedBeforeAnyWrite() {
    when(attemptMapper.selectForUpdateById(1L)).thenReturn(attempt("ready"));
    rejectAttemptScope();

    assertThatThrownBy(() -> service.finalizeAbsence(1L, LocalDateTime.of(2026, 8, 1, 10, 0)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("选手已归属其他赛段，不能处理当前赛段成绩");

    verifyNoInteractions(sessionMapper, answerMapper, questionMapper, paperMapper, auditMapper);
    verify(attemptMapper, never()).finishAbsent(any(), any(), any());
    verify(sourceMapper, never()).selectList(any());
    verify(sourceMapper, never()).insert(any(CompetitionScoreSourceEntity.class));
  }

  @Test
  void sourceReferenceConflictAcrossScopesIsRejectedBeforeScoringOrWriting() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 1, 10, 0);
    CompetitionScoreSourceEntity conflicting = new CompetitionScoreSourceEntity();
    conflicting.setTenantId(1L);
    conflicting.setEventId(100L);
    conflicting.setStageId(201L);
    conflicting.setEventWorkTypeId(300L);
    conflicting.setContestantMemberId(400L);
    conflicting.setSourceSystem("competition-theory-exam");
    conflicting.setSourceReference("theory-attempt:1");
    when(attemptMapper.selectForUpdateById(1L)).thenReturn(attempt("in_progress"));
    when(sourceMapper.selectList(any())).thenReturn(List.of(conflicting));

    assertThatThrownBy(() -> service.submit(1L, "auto", "auto:1", null, null, now))
        .isInstanceOf(BusinessException.class)
        .hasMessage("理论成绩来源引用与当前业务范围冲突");

    verifyNoInteractions(sessionMapper, answerMapper, questionMapper, paperMapper, auditMapper);
    verify(attemptMapper, never()).claimSubmission(any(), any(), any(), any(), any(), any());
    verify(attemptMapper, never()).finishSubmission(any(), any(), any(), any(), any());
    verify(sourceMapper, never()).insert(any(CompetitionScoreSourceEntity.class));
  }

  @Test
  void theorySubmissionAllowsContestantAssignedToAttemptStage() {
    CompetitionTheorySubmissionService legalService = serviceWithActualScope(STAGE_ID);
    configureSuccessfulAutoSubmission();

    CompetitionTheoryAttemptEntity result = legalService.submit(
        1L, "auto", "auto:legal-stage", null, null,
        LocalDateTime.of(2026, 8, 1, 10, 0));

    assertThat(result.getStatus()).isEqualTo("submitted_auto");
  }

  @Test
  void theorySubmissionAllowsContestantWithoutStageAssignment() {
    CompetitionTheorySubmissionService legalService = serviceWithActualScope(null);
    configureSuccessfulAutoSubmission();

    CompetitionTheoryAttemptEntity result = legalService.submit(
        1L, "auto", "auto:legal-unassigned", null, null,
        LocalDateTime.of(2026, 8, 1, 10, 0));

    assertThat(result.getStatus()).isEqualTo("submitted_auto");
  }

  private void rejectAttemptScope() {
    doThrow(new BusinessException(
        ErrorCode.UNPROCESSABLE_ENTITY, "选手已归属其他赛段，不能处理当前赛段成绩"))
        .when(contestantScopeValidator)
        .validate(1L, 100L, 200L, 300L, 400L);
  }

  private CompetitionTheorySubmissionService serviceWithActualScope(Long currentStageId) {
    CompetitionEventMemberMapper memberMapper = Mockito.mock(CompetitionEventMemberMapper.class);
    CompetitionContestantProfileMapper profileMapper =
        Mockito.mock(CompetitionContestantProfileMapper.class);
    when(memberMapper.selectOne(any())).thenReturn(validMember());
    when(profileMapper.selectOne(any())).thenReturn(validProfile(currentStageId));
    return new CompetitionTheorySubmissionService(
        attemptMapper, answerMapper, questionMapper, paperMapper, sessionMapper, sourceMapper,
        auditMapper, new CompetitionContestantScopeValidator(profileMapper, memberMapper),
        new ObjectMapper());
  }

  private void configureSuccessfulAutoSubmission() {
    when(attemptMapper.selectForUpdateById(1L)).thenReturn(
        attempt("in_progress"), attempt("submitted_auto"));
    when(sessionMapper.selectById(20L)).thenReturn(
        session(LocalDateTime.of(2026, 8, 1, 9, 0), LocalDateTime.of(2026, 8, 1, 11, 0)));
    when(attemptMapper.claimSubmission(
        eq(1L), eq("auto"), any(String.class), any(), any(), any()))
        .thenReturn(1);
    CompetitionTheoryPaperQuestionEntity question = question(101L, "single", "[\"A\"]");
    when(questionMapper.selectByPaper(30L)).thenReturn(List.of(question));
    when(answerMapper.selectByAttempt(1L)).thenReturn(List.of(answer(101L, "[\"A\"]")));
    when(answerMapper.updateById(any(CompetitionTheoryAnswerEntity.class))).thenReturn(1);
    CompetitionTheoryPaperSnapshotEntity paper = new CompetitionTheoryPaperSnapshotEntity();
    paper.setId(30L);
    paper.setScoreRuleId(40L);
    when(paperMapper.selectById(30L)).thenReturn(paper);
    when(sourceMapper.selectList(any())).thenReturn(List.of());
    when(sourceMapper.insert(any(CompetitionScoreSourceEntity.class))).thenAnswer(invocation -> {
      CompetitionScoreSourceEntity source = invocation.getArgument(0);
      source.setId(50L);
      return 1;
    });
    when(attemptMapper.finishSubmission(
        eq(1L), eq("submitted_auto"), any(), eq(50L), any())).thenReturn(1);
    when(auditMapper.insert(any(
        com.guoyu.platform.competition.entity.CompetitionTheoryExamSessionTokenAuditEntity.class)))
        .thenReturn(1);
  }

  private CompetitionEventMemberEntity validMember() {
    CompetitionEventMemberEntity member = new CompetitionEventMemberEntity();
    member.setId(400L);
    member.setTenantId(1L);
    member.setEventId(100L);
    member.setRoleType("contestant");
    member.setStatus("active");
    member.setDeleted(0);
    return member;
  }

  private CompetitionContestantProfileEntity validProfile(Long currentStageId) {
    CompetitionContestantProfileEntity profile = new CompetitionContestantProfileEntity();
    profile.setId(600L);
    profile.setTenantId(1L);
    profile.setMemberId(400L);
    profile.setEventWorkTypeId(300L);
    profile.setCurrentStageId(currentStageId);
    profile.setWorkTypeMatchStatus("matched");
    profile.setDeleted(0);
    return profile;
  }

  private CompetitionTheoryAttemptEntity attempt(String status) {
    CompetitionTheoryAttemptEntity attempt = new CompetitionTheoryAttemptEntity();
    attempt.setId(1L);
    attempt.setTenantId(1L);
    attempt.setAssignmentId(10L);
    attempt.setSessionId(20L);
    attempt.setPaperSnapshotId(30L);
    attempt.setEventId(100L);
    attempt.setStageId(200L);
    attempt.setEventWorkTypeId(300L);
    attempt.setContestantMemberId(400L);
    attempt.setPlayerId(500L);
    attempt.setStatus(status);
    return attempt;
  }

  private CompetitionTheoryExamSessionEntity session(LocalDateTime start, LocalDateTime end) {
    CompetitionTheoryExamSessionEntity session = new CompetitionTheoryExamSessionEntity();
    session.setId(20L);
    session.setStartAt(start);
    session.setEndAt(end);
    return session;
  }

  private CompetitionTheoryPaperQuestionEntity question(Long id, String type, String correctJson) {
    CompetitionTheoryPaperQuestionEntity question = new CompetitionTheoryPaperQuestionEntity();
    question.setId(id);
    question.setPaperSnapshotId(30L);
    question.setQuestionType(type);
    question.setCorrectAnswersJson(correctJson);
    question.setScore(new BigDecimal("50"));
    question.setScoringRuleCode("exact-set");
    question.setScoringRuleVersion(1);
    return question;
  }

  private CompetitionTheoryAnswerEntity answer(Long questionId, String answerJson) {
    CompetitionTheoryAnswerEntity answer = new CompetitionTheoryAnswerEntity();
    answer.setId(questionId + 1000);
    answer.setAttemptId(1L);
    answer.setQuestionSnapshotId(questionId);
    answer.setAnswerJson(answerJson);
    answer.setSaveVersion(1);
    return answer;
  }
}
