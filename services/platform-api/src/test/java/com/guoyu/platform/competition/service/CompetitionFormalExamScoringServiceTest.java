package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.exam.formal.scoring.FormalExamScoringException;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionFormalExamScoringServiceTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-08T03:00:00Z"), ZONE);
  private static final LocalDateTime NOW = LocalDateTime.ofInstant(Instant.parse("2026-09-08T03:00:00Z"), ZONE);

  private final CompetitionFormalExamSessionMapper sessions =
      Mockito.mock(CompetitionFormalExamSessionMapper.class);
  private final CompetitionFormalCandidateExamMapper candidates =
      Mockito.mock(CompetitionFormalCandidateExamMapper.class);
  private final CompetitionFormalExamPaperSnapshotMapper papers =
      Mockito.mock(CompetitionFormalExamPaperSnapshotMapper.class);
  private final CompetitionFormalExamQuestionSnapshotMapper questions =
      Mockito.mock(CompetitionFormalExamQuestionSnapshotMapper.class);
  private final CompetitionFormalExamCandidateScoringService candidateScoring =
      Mockito.mock(CompetitionFormalExamCandidateScoringService.class);

  private final CompetitionFormalExamScoringService service =
      new CompetitionFormalExamScoringService(
          sessions, candidates, papers, questions, candidateScoring, CLOCK, 15);

  @Test
  void runningSessionIsRejectedByAdminRetry() {
    when(sessions.selectById(9001L)).thenReturn(session("running", "not_started", 2));
    assertThatThrownBy(() -> service.retryFromAdmin(9001L))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前比赛尚未结束，不能评分");
    verify(sessions, never()).markScoringIfIdle(any(), any(), any());
  }

  @Test
  void completedScoringIsIdempotent() {
    when(sessions.selectById(9001L)).thenReturn(session("finished", "completed", 2));
    service.retryFromAdmin(9001L);
    verify(sessions, never()).markScoringIfIdle(any(), any(), any());
    verify(candidateScoring, never()).scoreCandidate(any(), any(), any());
  }

  @Test
  void malformedQuestionFailsSessionButKeepsFinished() {
    CompetitionFormalExamSessionEntity finished = session("finished", "not_started", 2);
    when(sessions.selectById(9001L)).thenReturn(finished);
    when(sessions.markScoringIfIdle(9001L, NOW, NOW)).thenReturn(1);
    when(candidates.selectByExamSessionId(9001L)).thenReturn(List.of(
        candidate(9101L, "submitted"), candidate(9102L, "submitted")));
    stubPaper();
    when(candidateScoring.scoreCandidate(9001L, 9101L, NOW))
        .thenReturn(CompetitionFormalExamCandidateScoringService.Outcome.SCORED);
    when(candidateScoring.scoreCandidate(9001L, 9102L, NOW))
        .thenThrow(new FormalExamScoringException("正式题目标准答案格式异常", 9001L, 9102L, 9302L));
    when(sessions.markScoringFailed(eq(9001L), any(), eq(NOW))).thenReturn(1);

    service.scoreFinishedSession(9001L);

    verify(candidateScoring).scoreCandidate(9001L, 9101L, NOW);
    verify(sessions).markScoringFailed(eq(9001L), eq("正式题目标准答案格式异常"), eq(NOW));
    verify(sessions, never()).markScoringCompleted(any(), any(), any());
    verify(sessions, never()).markFinishedIfRunning(any(), any(), any(), any(), any(), any());
  }

  @Test
  void retryScoresOnlyTheFailedCandidate() {
    CompetitionFormalExamSessionEntity failed = session("finished", "failed", 2);
    when(sessions.selectById(9001L)).thenReturn(failed);
    when(sessions.markScoringIfIdle(9001L, NOW, NOW)).thenReturn(1);
    when(candidates.selectByExamSessionId(9001L)).thenReturn(List.of(
        candidate(9101L, "submitted"), candidate(9102L, "submitted")));
    stubPaper();
    when(candidateScoring.scoreCandidate(9001L, 9101L, NOW))
        .thenReturn(CompetitionFormalExamCandidateScoringService.Outcome.SKIPPED);
    when(candidateScoring.scoreCandidate(9001L, 9102L, NOW))
        .thenReturn(CompetitionFormalExamCandidateScoringService.Outcome.SCORED);
    when(sessions.markScoringCompleted(9001L, NOW, NOW)).thenReturn(1);

    service.scoreFinishedSession(9001L);

    verify(candidateScoring).scoreCandidate(9001L, 9101L, NOW);
    verify(candidateScoring).scoreCandidate(9001L, 9102L, NOW);
    verify(sessions).markScoringCompleted(9001L, NOW, NOW);
  }

  @Test
  void twoWorkersOnlyOneAcquiresScoring() {
    CompetitionFormalExamSessionEntity finished = session("finished", "not_started", 1);
    when(sessions.selectById(9001L)).thenReturn(finished);
    AtomicInteger cas = new AtomicInteger();
    when(sessions.markScoringIfIdle(9001L, NOW, NOW)).thenAnswer(invocation -> cas.getAndIncrement() == 0 ? 1 : 0);
    when(candidates.selectByExamSessionId(9001L)).thenReturn(List.of(candidate(9101L, "absent")));
    stubPaper();
    when(candidateScoring.scoreCandidate(9001L, 9101L, NOW))
        .thenReturn(CompetitionFormalExamCandidateScoringService.Outcome.ABSENT);
    when(sessions.markScoringCompleted(9001L, NOW, NOW)).thenReturn(1);

    service.scoreFinishedSession(9001L);
    service.scoreFinishedSession(9001L);

    verify(sessions, times(2)).markScoringIfIdle(9001L, NOW, NOW);
    verify(candidateScoring, times(1)).scoreCandidate(9001L, 9101L, NOW);
  }

  @Test
  void absentAndSubmittedAreBothProcessed() {
    CompetitionFormalExamSessionEntity finished = session("finished", "not_started", 2);
    when(sessions.selectById(9001L)).thenReturn(finished);
    when(sessions.markScoringIfIdle(9001L, NOW, NOW)).thenReturn(1);
    when(candidates.selectByExamSessionId(9001L)).thenReturn(List.of(
        candidate(9101L, "submitted"), candidate(9104L, "absent")));
    stubPaper();
    when(candidateScoring.scoreCandidate(9001L, 9101L, NOW))
        .thenReturn(CompetitionFormalExamCandidateScoringService.Outcome.SCORED);
    when(candidateScoring.scoreCandidate(9001L, 9104L, NOW))
        .thenReturn(CompetitionFormalExamCandidateScoringService.Outcome.ABSENT);
    when(sessions.markScoringCompleted(9001L, NOW, NOW)).thenReturn(1);

    service.scoreFinishedSession(9001L);

    verify(sessions).markScoringCompleted(9001L, NOW, NOW);
  }

  private void stubPaper() {
    CompetitionFormalExamPaperSnapshotEntity paper = new CompetitionFormalExamPaperSnapshotEntity();
    paper.setId(9201L);
    paper.setExamSessionId(9001L);
    paper.setQuestionCount(1);
    paper.setTotalScore(new BigDecimal("1.00"));
    when(papers.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(paper));
    CompetitionFormalExamQuestionSnapshotEntity question =
        new CompetitionFormalExamQuestionSnapshotEntity();
    question.setId(9301L);
    question.setScore(new BigDecimal("1.00"));
    when(questions.selectByPaperSnapshotId(9201L)).thenReturn(List.of(question));
  }

  private CompetitionFormalExamSessionEntity session(String status, String scoringStatus, int total) {
    CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
    session.setId(9001L);
    session.setStatus(status);
    session.setScoringStatus(scoringStatus);
    session.setCandidateTotal(total);
    return session;
  }

  private CompetitionFormalCandidateExamEntity candidate(Long id, String status) {
    CompetitionFormalCandidateExamEntity entity = new CompetitionFormalCandidateExamEntity();
    entity.setId(id);
    entity.setExamSessionId(9001L);
    entity.setStatus(status);
    return entity;
  }
}
