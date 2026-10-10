package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.entity.CompetitionStageExecutionPlanEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionMakeExamPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalJudgeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalJudgeMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalRuleItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreEntryMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeAvoidanceMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreRuleMapper;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageExecutionPlanMapper;
import com.guoyu.platform.competition.domain.CompetitionStageLifecycleGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CompetitionPracticalExecutionScoreRuleTest {
  private final CompetitionStageExecutionPlanMapper planMapper =
      Mockito.mock(CompetitionStageExecutionPlanMapper.class);
  private final CompetitionScoreRuleMapper scoreRuleMapper =
      Mockito.mock(CompetitionScoreRuleMapper.class);
  private final CompetitionPracticalSessionMapper sessionMapper =
      Mockito.mock(CompetitionPracticalSessionMapper.class);
  private final CompetitionPracticalAssignmentMapper assignmentMapper =
      Mockito.mock(CompetitionPracticalAssignmentMapper.class);
  private final CompetitionPracticalScoreSheetMapper sheetMapper =
      Mockito.mock(CompetitionPracticalScoreSheetMapper.class);
  private final CompetitionPracticalScoreMapper practicalScoreMapper =
      Mockito.mock(CompetitionPracticalScoreMapper.class);
  private final CompetitionScoreSourceMapper scoreSourceMapper =
      Mockito.mock(CompetitionScoreSourceMapper.class);
  private final CompetitionMakeExamPaperQuestionMapper paperQuestionMapper =
      Mockito.mock(CompetitionMakeExamPaperQuestionMapper.class);

  private final CompetitionPracticalExecutionService service = new CompetitionPracticalExecutionService(
      sessionMapper,
      Mockito.mock(CompetitionPracticalJudgeGroupMapper.class),
      Mockito.mock(CompetitionPracticalJudgeMemberMapper.class),
      assignmentMapper,
      Mockito.mock(CompetitionPracticalRuleMapper.class),
      Mockito.mock(CompetitionPracticalRuleItemMapper.class),
      sheetMapper,
      Mockito.mock(CompetitionPracticalScoreEntryMapper.class),
      practicalScoreMapper,
      Mockito.mock(CompetitionRefereeAvoidanceMapper.class),
      scoreRuleMapper,
      scoreSourceMapper,
      planMapper,
      Mockito.mock(CompetitionStageLifecycleGuard.class),
      paperQuestionMapper,
      new ObjectMapper(),
      Mockito.mock(CompetitionEventMemberMapper.class),
      null);

  @Test
  void freezeBindsUniquePublishedScoreRuleOntoRuntimePlan() {
    CompetitionStageExecutionPlanEntity plan = new CompetitionStageExecutionPlanEntity();
    plan.setId(200L);
    plan.setEventId(20L);
    plan.setStageId(10L);
    plan.setEventWorkTypeId(30L);
    plan.setEventWorkTypeLevelId(40L);
    plan.setStatus("published");
    CompetitionScoreRuleEntity rule = new CompetitionScoreRuleEntity();
    rule.setId(88L);
    when(scoreRuleMapper.selectList(any())).thenReturn(List.of(rule));
    when(planMapper.updateById(plan)).thenReturn(1);

    Long frozen = service.freezePlanScoreRule(10159L, 1000L, plan);

    assertThat(frozen).isEqualTo(88L);
    assertThat(plan.getScoreRuleId()).isEqualTo(88L);
    verify(planMapper).updateById(plan);
  }

  @Test
  void freezeKeepsExistingScoreRule() {
    CompetitionStageExecutionPlanEntity plan = new CompetitionStageExecutionPlanEntity();
    plan.setScoreRuleId(77L);

    Long frozen = service.freezePlanScoreRule(10159L, 1000L, plan);

    assertThat(frozen).isEqualTo(77L);
    Mockito.verifyNoInteractions(scoreRuleMapper);
  }

  @Test
  void leaderCompletionDoesNotRequirePublishedScoreRule() {
    CompetitionPracticalScoreSheetEntity sheet = new CompetitionPracticalScoreSheetEntity();
    sheet.setId(300L);
    sheet.setAssignmentId(400L);
    sheet.setPlanId(200L);
    sheet.setEventId(20L);
    sheet.setStageId(10L);
    sheet.setEventWorkTypeId(30L);
    sheet.setEventWorkTypeLevelId(40L);
    sheet.setContestantMemberId(50L);
    sheet.setStatus("submitted_locked");
    when(sheetMapper.selectOne(any())).thenReturn(sheet);

    CompetitionStageExecutionPlanEntity plan = new CompetitionStageExecutionPlanEntity();
    plan.setId(200L);
    plan.setEventId(20L);
    plan.setStageId(10L);
    plan.setEventWorkTypeId(30L);
    plan.setEventWorkTypeLevelId(40L);
    plan.setStatus("published");
    when(planMapper.selectOne(any())).thenReturn(plan);

    CompetitionPracticalAssignmentEntity assignment = new CompetitionPracticalAssignmentEntity();
    assignment.setSessionId(600L);
    when(assignmentMapper.selectOne(any())).thenReturn(assignment);
    CompetitionPracticalSessionEntity session = new CompetitionPracticalSessionEntity();
    session.setId(600L);
    when(sessionMapper.selectOne(any())).thenReturn(session);

    when(practicalScoreMapper.selectList(any())).thenReturn(List.of(
        refereeScore(1L, "90.00"),
        refereeScore(2L, "92.00"),
        refereeScore(3L, "94.00")));
    when(scoreSourceMapper.insert(any(CompetitionScoreSourceEntity.class))).thenReturn(1);

    CompetitionScoreSourceEntity result =
        service.completePracticalScore(10159L, 1000L, 400L);

    assertThat(result.getRawScore()).isEqualByComparingTo("92.00");
    assertThat(result.getScoreRuleId()).isNull();
    assertThat(result.getConfirmedBy()).isEqualTo(1000L);
    ArgumentCaptor<CompetitionScoreSourceEntity> sourceCaptor =
        ArgumentCaptor.forClass(CompetitionScoreSourceEntity.class);
    verify(scoreSourceMapper).insert(sourceCaptor.capture());
    assertThat(sourceCaptor.getValue().getSourceReference())
        .startsWith("practical-average:leader-confirmed:");
    Mockito.verifyNoInteractions(scoreRuleMapper);
  }

  @Test
  void previewWarnsAsSoonAsTwoSubmittedScoresExceedFrozenQuestionThreshold() {
    CompetitionPracticalAssignmentEntity assignment = new CompetitionPracticalAssignmentEntity();
    assignment.setSessionId(600L);
    when(assignmentMapper.selectOne(any())).thenReturn(assignment);
    CompetitionPracticalSessionEntity session = new CompetitionPracticalSessionEntity();
    session.setId(600L);
    session.setPaperSnapshotId(700L);
    when(sessionMapper.selectOne(any())).thenReturn(session);
    CompetitionMakeExamPaperQuestionEntity question = new CompetitionMakeExamPaperQuestionEntity();
    question.setQuestionType("practical");
    question.setScoreDifference(new java.math.BigDecimal("10"));
    when(paperQuestionMapper.selectByPaper(10159L, 700L)).thenReturn(List.of(question));

    var preview = service.previewPracticalScoreDifference(
        10159L, 200L, 50L,
        List.of(new java.math.BigDecimal("80"), new java.math.BigDecimal("95")));

    assertThat(preview.status()).isEqualTo("exceeded");
    assertThat(preview.actualDifference()).isEqualByComparingTo("15");
    assertThat(preview.allowedDifference()).isEqualByComparingTo("10");
    assertThat(preview.submittedRefereeCount()).isEqualTo(2);
    assertThat(preview.exceeded()).isTrue();
  }

  private CompetitionPracticalScoreEntity refereeScore(Long refereeId, String score) {
    CompetitionPracticalScoreEntity row = new CompetitionPracticalScoreEntity();
    row.setRefereeMemberId(refereeId);
    row.setTotalScore(new java.math.BigDecimal(score));
    row.setCurrentFlag(1);
    return row;
  }
}
