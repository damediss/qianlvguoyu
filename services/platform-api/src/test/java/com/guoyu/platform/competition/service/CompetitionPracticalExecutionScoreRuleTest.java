package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.entity.CompetitionScoreRuleEntity;
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
import org.mockito.Mockito;

class CompetitionPracticalExecutionScoreRuleTest {
  private final CompetitionStageExecutionPlanMapper planMapper =
      Mockito.mock(CompetitionStageExecutionPlanMapper.class);
  private final CompetitionScoreRuleMapper scoreRuleMapper =
      Mockito.mock(CompetitionScoreRuleMapper.class);

  private final CompetitionPracticalExecutionService service = new CompetitionPracticalExecutionService(
      Mockito.mock(CompetitionPracticalSessionMapper.class),
      Mockito.mock(CompetitionPracticalJudgeGroupMapper.class),
      Mockito.mock(CompetitionPracticalJudgeMemberMapper.class),
      Mockito.mock(CompetitionPracticalAssignmentMapper.class),
      Mockito.mock(CompetitionPracticalRuleMapper.class),
      Mockito.mock(CompetitionPracticalRuleItemMapper.class),
      Mockito.mock(CompetitionPracticalScoreSheetMapper.class),
      Mockito.mock(CompetitionPracticalScoreEntryMapper.class),
      Mockito.mock(CompetitionPracticalScoreMapper.class),
      Mockito.mock(CompetitionRefereeAvoidanceMapper.class),
      scoreRuleMapper,
      Mockito.mock(CompetitionScoreSourceMapper.class),
      planMapper,
      Mockito.mock(CompetitionStageLifecycleGuard.class),
      Mockito.mock(CompetitionMakeExamPaperQuestionMapper.class),
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
}
