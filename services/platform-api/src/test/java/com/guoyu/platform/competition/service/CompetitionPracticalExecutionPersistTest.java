package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.domain.CompetitionStageLifecycleGuard;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreEntryReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreSaveReq;
import com.guoyu.platform.competition.entity.CompetitionMakeExamPaperQuestionEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalJudgeMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalSessionEntity;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompetitionPracticalExecutionPersistTest {
  private static final Long TENANT_ID = 10159L;
  private static final Long USER_ID = 1000L;
  private static final Long SHEET_ID = 2101601341466050561L;
  private static final Long QUESTION_ID = 2101518641891766274L;
  private static final Long ASSIGNMENT_ID = 11L;
  private static final Long SESSION_ID = 22L;
  private static final Long PLAN_ID = 33L;
  private static final Long GROUP_ID = 44L;
  private static final Long REFEREE_ID = 55L;
  private static final Long PAPER_ID = 66L;

  private final CompetitionPracticalSessionMapper sessionMapper =
      Mockito.mock(CompetitionPracticalSessionMapper.class);
  private final CompetitionPracticalJudgeMemberMapper memberMapper =
      Mockito.mock(CompetitionPracticalJudgeMemberMapper.class);
  private final CompetitionPracticalAssignmentMapper assignmentMapper =
      Mockito.mock(CompetitionPracticalAssignmentMapper.class);
  private final CompetitionPracticalScoreSheetMapper sheetMapper =
      Mockito.mock(CompetitionPracticalScoreSheetMapper.class);
  private final CompetitionPracticalScoreEntryMapper entryMapper =
      Mockito.mock(CompetitionPracticalScoreEntryMapper.class);
  private final CompetitionMakeExamPaperQuestionMapper paperQuestionMapper =
      Mockito.mock(CompetitionMakeExamPaperQuestionMapper.class);
  private final CompetitionStageLifecycleGuard lifecycleGuard =
      Mockito.mock(CompetitionStageLifecycleGuard.class);

  private final List<CompetitionPracticalScoreEntryEntity> liveEntries = new ArrayList<>();
  private CompetitionPracticalExecutionService service;

  @BeforeEach
  void setUp() {
    liveEntries.clear();
    service = new CompetitionPracticalExecutionService(
        sessionMapper,
        Mockito.mock(CompetitionPracticalJudgeGroupMapper.class),
        memberMapper,
        assignmentMapper,
        Mockito.mock(CompetitionPracticalRuleMapper.class),
        Mockito.mock(CompetitionPracticalRuleItemMapper.class),
        sheetMapper,
        entryMapper,
        Mockito.mock(CompetitionPracticalScoreMapper.class),
        Mockito.mock(CompetitionRefereeAvoidanceMapper.class),
        Mockito.mock(CompetitionScoreRuleMapper.class),
        Mockito.mock(CompetitionScoreSourceMapper.class),
        Mockito.mock(CompetitionStageExecutionPlanMapper.class),
        lifecycleGuard,
        paperQuestionMapper,
        new ObjectMapper(),
        Mockito.mock(CompetitionEventMemberMapper.class),
        null);

    CompetitionPracticalScoreSheetEntity sheet = new CompetitionPracticalScoreSheetEntity();
    sheet.setId(SHEET_ID);
    sheet.setTenantId(TENANT_ID);
    sheet.setAssignmentId(ASSIGNMENT_ID);
    sheet.setPlanId(PLAN_ID);
    sheet.setEventId(1L);
    sheet.setStageId(2L);
    sheet.setEventWorkTypeId(3L);
    sheet.setEventWorkTypeLevelId(4L);
    sheet.setContestantMemberId(77L);
    sheet.setRefereeMemberId(REFEREE_ID);
    sheet.setStatus("draft");
    when(sheetMapper.selectOne(any())).thenReturn(sheet);
    when(sheetMapper.updateById(any(CompetitionPracticalScoreSheetEntity.class))).thenReturn(1);

    CompetitionPracticalAssignmentEntity assignment = new CompetitionPracticalAssignmentEntity();
    assignment.setId(ASSIGNMENT_ID);
    assignment.setTenantId(TENANT_ID);
    assignment.setPlanId(PLAN_ID);
    assignment.setSessionId(SESSION_ID);
    assignment.setJudgeGroupId(GROUP_ID);
    assignment.setRosterItemId(88L);
    assignment.setContestantMemberId(77L);
    assignment.setRefereeMemberId(REFEREE_ID);
    when(assignmentMapper.selectOne(any())).thenReturn(assignment);

    CompetitionPracticalSessionEntity session = new CompetitionPracticalSessionEntity();
    session.setId(SESSION_ID);
    session.setTenantId(TENANT_ID);
    session.setPlanId(PLAN_ID);
    session.setEventId(1L);
    session.setStageId(2L);
    session.setEventWorkTypeId(3L);
    session.setEventWorkTypeLevelId(4L);
    session.setPaperSnapshotId(PAPER_ID);
    when(sessionMapper.selectOne(any())).thenReturn(session);

    CompetitionStageExecutionPlanEntity plan = new CompetitionStageExecutionPlanEntity();
    plan.setId(PLAN_ID);
    when(lifecycleGuard.requirePublishedPlan(any(), any(), any(), any(), any(), any()))
        .thenReturn(plan);

    CompetitionPracticalJudgeMemberEntity member = new CompetitionPracticalJudgeMemberEntity();
    member.setJudgeGroupId(GROUP_ID);
    member.setRefereeMemberId(REFEREE_ID);
    member.setMemberRole("official");
    member.setStatus("active");
    when(memberMapper.selectList(any())).thenReturn(List.of(member));

    CompetitionMakeExamPaperQuestionEntity question = new CompetitionMakeExamPaperQuestionEntity();
    question.setId(QUESTION_ID);
    question.setQuestionType("practical");
    question.setScoringItemsJson(
        "[{\"itemNo\":1,\"maxScore\":10,\"maxDeduction\":10,\"deductionStep\":0.5,\"scoringMethod\":\"deductive\"}]");
    when(paperQuestionMapper.selectByPaper(TENANT_ID, PAPER_ID)).thenReturn(List.of(question));

    when(entryMapper.selectList(any())).thenAnswer(invocation -> new ArrayList<>(liveEntries));
    when(entryMapper.insert(any(CompetitionPracticalScoreEntryEntity.class))).thenAnswer(invocation -> {
      liveEntries.add(invocation.getArgument(0));
      return 1;
    });
    when(entryMapper.updateById(any(CompetitionPracticalScoreEntryEntity.class))).thenReturn(1);
    when(entryMapper.deleteById(anyLong())).thenAnswer(invocation -> {
      Long id = invocation.getArgument(0);
      boolean removed = liveEntries.removeIf(item -> Objects.equals(item.getId(), id));
      return removed ? 1 : 0;
    });
  }

  @Test
  void repeatedDraftSaveUpdatesExistingSnapshotEntryInsteadOfDeleting() {
    CompetitionPracticalScoreSaveReq first = saveReq(BigDecimal.ZERO);
    service.saveSheet(TENANT_ID, USER_ID, SHEET_ID, first);
    assertThat(liveEntries).hasSize(1);
    Long entryId = liveEntries.get(0).getId();

    CompetitionPracticalScoreSaveReq second = saveReq(new BigDecimal("0.50"));
    CompetitionPracticalScoreSheetEntity sheet =
        service.saveSheet(TENANT_ID, USER_ID, SHEET_ID, second);

    assertThat(liveEntries).hasSize(1);
    assertThat(liveEntries.get(0).getId()).isEqualTo(entryId);
    assertThat(liveEntries.get(0).getDeduction()).isEqualByComparingTo("0.50");
    assertThat(liveEntries.get(0).getScore()).isEqualByComparingTo("9.50");
    assertThat(sheet.getTotalScore()).isEqualByComparingTo("9.50");
    verify(entryMapper, never()).deleteById(anyLong());
  }

  private CompetitionPracticalScoreSaveReq saveReq(BigDecimal deduction) {
    BigDecimal score = new BigDecimal("10.00").subtract(deduction);
    return new CompetitionPracticalScoreSaveReq(List.of(
        new CompetitionPracticalScoreEntryReq(
            null, QUESTION_ID, 1, score, deduction, "扣分")));
  }
}
