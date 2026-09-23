package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionPracticalJudgeGroupReq;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssessmentSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalJudgeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreEntryEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.entity.CompetitionRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageExecutionPlanEntity;
import com.guoyu.platform.competition.entity.CompetitionStagePaperAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterItemEntity;
import com.guoyu.platform.competition.entity.CompetitionStageRosterSnapshotEntity;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssessmentSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreEntryMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.mapper.CompetitionRosterItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageExecutionPlanMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageRosterSnapshotMapper;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionPracticalConsoleServiceTest {
  private static final Long TENANT_ID = 10159L;
  private static final Long USER_ID = 1000L;
  private static final Long EVENT_ID = 20L;
  private static final Long STAGE_ID = 10L;
  private static final Long WORK_TYPE_ID = 30L;
  private static final Long LEVEL_ID = 40L;
  private static final Long CONTESTANT_ID = 500L;

  private final CompetitionStageApplicationService stageService =
      Mockito.mock(CompetitionStageApplicationService.class);
  private final CompetitionStageExecutionPlanMapper planMapper =
      Mockito.mock(CompetitionStageExecutionPlanMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levelMapper =
      Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionStagePaperAssignmentMapper paperAssignmentMapper =
      Mockito.mock(CompetitionStagePaperAssignmentMapper.class);
  private final CompetitionStageRosterItemMapper rosterItemMapper =
      Mockito.mock(CompetitionStageRosterItemMapper.class);
  private final CompetitionStageRosterSnapshotMapper rosterSnapshotMapper =
      Mockito.mock(CompetitionStageRosterSnapshotMapper.class);
  private final CompetitionRefereeAssignmentMapper refereeAssignmentMapper =
      Mockito.mock(CompetitionRefereeAssignmentMapper.class);
  private final CompetitionRefereeGroupMapper refereeGroupMapper =
      Mockito.mock(CompetitionRefereeGroupMapper.class);
  private final CompetitionPracticalSessionMapper sessionMapper =
      Mockito.mock(CompetitionPracticalSessionMapper.class);
  private final CompetitionPracticalAssignmentMapper practicalAssignmentMapper =
      Mockito.mock(CompetitionPracticalAssignmentMapper.class);
  private final CompetitionPracticalScoreSheetMapper scoreSheetMapper =
      Mockito.mock(CompetitionPracticalScoreSheetMapper.class);
  private final CompetitionPracticalExecutionService executionService =
      Mockito.mock(CompetitionPracticalExecutionService.class);
  private final CompetitionContestantProfileMapper contestantProfileMapper =
      Mockito.mock(CompetitionContestantProfileMapper.class);
  private final CompetitionRegistrationMapper registrationMapper =
      Mockito.mock(CompetitionRegistrationMapper.class);
  private final CompetitionRosterItemMapper rosterNameMapper =
      Mockito.mock(CompetitionRosterItemMapper.class);
  private final CompetitionEventMemberMapper eventMemberMapper =
      Mockito.mock(CompetitionEventMemberMapper.class);
  private final CompetitionRefereeProfileMapper refereeProfileMapper =
      Mockito.mock(CompetitionRefereeProfileMapper.class);
  private final CompetitionPracticalAssessmentSheetMapper assessmentSheetMapper =
      Mockito.mock(CompetitionPracticalAssessmentSheetMapper.class);
  private final CompetitionPracticalScoreEntryMapper scoreEntryMapper =
      Mockito.mock(CompetitionPracticalScoreEntryMapper.class);

  private final CompetitionPracticalConsoleService service =
      new CompetitionPracticalConsoleService(
          stageService, planMapper, levelMapper, paperAssignmentMapper, rosterItemMapper,
          rosterSnapshotMapper, refereeAssignmentMapper, refereeGroupMapper, sessionMapper,
          practicalAssignmentMapper, scoreSheetMapper, executionService,
          contestantProfileMapper, registrationMapper, rosterNameMapper, eventMemberMapper,
          refereeProfileMapper, assessmentSheetMapper, scoreEntryMapper);

  @BeforeEach
  void authenticate() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        USER_ID, TENANT_ID, "admin", "管理员", "platform",
        List.of("PLATFORM_ADMIN"), List.of("competition:practical-console:manage"), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
    when(executionService.previewPracticalScoreDifference(any(), any(), any(), any()))
        .thenReturn(new CompetitionPracticalExecutionService.PracticalScoreDifferencePreview(
            new BigDecimal("10"), null, 1, "waiting", false));
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void initializeCreatesRuntimePlanAndRosterWithoutPublishedPlan() {
    stubInitializeDependencies(stage("configuring"));

    var result = service.initialize(STAGE_ID, LEVEL_ID);

    assertThat(result.status()).isEqualTo("initialized");
    assertThat(result.scoringTaskCount()).isEqualTo(3);
    verify(planMapper).insert(any(CompetitionStageExecutionPlanEntity.class));
    verify(executionService).freezePlanScoreRule(eq(TENANT_ID), eq(USER_ID), any());
    verify(rosterSnapshotMapper).insert(any(CompetitionStageRosterSnapshotEntity.class));
    ArgumentCaptor<CompetitionPracticalJudgeGroupReq> groupCaptor =
        ArgumentCaptor.forClass(CompetitionPracticalJudgeGroupReq.class);
    verify(executionService).createGroup(any(), any(), any(), groupCaptor.capture(), any());
    assertThat(groupCaptor.getValue().members())
        .extracting(member -> member.memberRole())
        .containsExactly("leader", "member", "member");
    verify(stageService, never()).beginConfiguration(any(), any());
  }

  @Test
  void initializePromotesDraftStageBeforeCreatingSessions() {
    CompetitionStageVO draft = stage("draft");
    CompetitionStageVO configuring = stage("configuring");
    stubInitializeDependencies(draft);
    when(stageService.beginConfiguration(eq(STAGE_ID), eq("实操正式比赛初始化")))
        .thenReturn(configuring);

    var result = service.initialize(STAGE_ID, LEVEL_ID);

    assertThat(result.status()).isEqualTo("initialized");
    verify(stageService).beginConfiguration(STAGE_ID, "实操正式比赛初始化");
    verify(executionService).publishSession(any(), any());
  }

  @Test
  void getGroupsRefereeTasksWithContestantNamesAndQuestionScores() {
    when(stageService.get(STAGE_ID)).thenReturn(stage("configuring"));
    CompetitionStageExecutionPlanEntity plan = new CompetitionStageExecutionPlanEntity();
    plan.setId(200L);
    plan.setTenantId(TENANT_ID);
    plan.setEventId(EVENT_ID);
    plan.setStageId(STAGE_ID);
    plan.setEventWorkTypeId(WORK_TYPE_ID);
    plan.setEventWorkTypeLevelId(LEVEL_ID);
    plan.setStatus("published");
    plan.setDeleted(0);
    when(planMapper.selectOne(any())).thenReturn(plan);

    CompetitionPracticalSessionEntity session = new CompetitionPracticalSessionEntity();
    session.setId(300L);
    when(sessionMapper.selectList(any())).thenReturn(List.of(session));
    when(practicalAssignmentMapper.selectList(any())).thenReturn(List.of(
        assignment(501L, 1001L), assignment(502L, 1002L), assignment(503L, 1003L)));

    CompetitionPracticalScoreSheetEntity sheet = new CompetitionPracticalScoreSheetEntity();
    sheet.setId(800L);
    sheet.setAssignmentId(501L);
    sheet.setStatus("submitted_locked");
    sheet.setTotalScore(new BigDecimal("35.0"));
    when(scoreSheetMapper.selectList(any())).thenReturn(List.of(sheet));

    CompetitionContestantProfileEntity profile = new CompetitionContestantProfileEntity();
    profile.setMemberId(CONTESTANT_ID);
    profile.setContestantNo("C-001");
    profile.setRegistrationId(700L);
    profile.setEventWorkTypeId(WORK_TYPE_ID);
    profile.setDeleted(0);
    when(contestantProfileMapper.selectList(any())).thenReturn(List.of(profile));

    CompetitionRegistrationEntity registration = new CompetitionRegistrationEntity();
    registration.setId(700L);
    registration.setRosterItemId(70L);
    registration.setDeleted(0);
    when(registrationMapper.selectList(any())).thenReturn(List.of(registration));

    CompetitionRosterItemEntity roster = new CompetitionRosterItemEntity();
    roster.setId(70L);
    roster.setName("张三");
    roster.setDeleted(0);
    when(rosterNameMapper.selectList(any())).thenReturn(List.of(roster));

    CompetitionEventMemberEntity refereeMember = new CompetitionEventMemberEntity();
    refereeMember.setId(1001L);
    refereeMember.setPersonId(11L);
    refereeMember.setDeleted(0);
    when(eventMemberMapper.selectList(any())).thenReturn(List.of(refereeMember));

    CompetitionRefereeProfileEntity refereeProfile = new CompetitionRefereeProfileEntity();
    refereeProfile.setPersonId(11L);
    refereeProfile.setRefereeName("认定测试001");
    refereeProfile.setDeleted(0);
    when(refereeProfileMapper.selectList(any())).thenReturn(List.of(refereeProfile));

    CompetitionPracticalAssessmentSheetEntity assessment = new CompetitionPracticalAssessmentSheetEntity();
    assessment.setId(900L);
    assessment.setTitle("实操考评表");
    assessment.setSheetJson("""
        {"summary":[{"paperQuestionId":"1","sortOrder":1,"title":"试题1","score":35}]}
        """);
    assessment.setDeleted(0);
    when(assessmentSheetMapper.selectList(any())).thenReturn(List.of(assessment));

    CompetitionPracticalScoreEntryEntity entry = new CompetitionPracticalScoreEntryEntity();
    entry.setScoreSheetId(800L);
    entry.setPaperQuestionId(1L);
    entry.setScore(new BigDecimal("35"));
    when(scoreEntryMapper.selectList(any())).thenReturn(List.of(entry));
    when(executionService.previewPracticalScoreDifference(any(), any(), any(), any()))
        .thenReturn(new CompetitionPracticalExecutionService.PracticalScoreDifferencePreview(
            new BigDecimal("10"), new BigDecimal("15"), 2, "exceeded", true));

    var result = service.get(STAGE_ID, LEVEL_ID);

    assertThat(result.contestantCount()).isEqualTo(1);
    assertThat(result.tasks()).hasSize(3);
    var first = result.tasks().stream()
        .filter(item -> "501".equals(item.assignmentId()))
        .findFirst()
        .orElseThrow();
    assertThat(first.contestantNo()).isEqualTo("C-001");
    assertThat(first.contestantName()).isEqualTo("张三");
    assertThat(first.refereeName()).isEqualTo("认定测试001");
    assertThat(first.assessmentSheetTitle()).isEqualTo("实操考评表");
    assertThat(first.questions()).hasSize(1);
    assertThat(first.questions().get(0).title()).isEqualTo("试题1");
    assertThat(first.questions().get(0).score()).isEqualByComparingTo("35");
    assertThat(first.questions().get(0).status()).isEqualTo("submitted");
    assertThat(first.scoreSheetId()).isEqualTo("800");
    assertThat(first.scoreDifferenceLimit()).isEqualByComparingTo("10");
    assertThat(first.actualScoreDifference()).isEqualByComparingTo("15");
    assertThat(first.scoreDifferenceStatus()).isEqualTo("exceeded");
  }

  @Test
  void returnForRescoreUnlocksSheetAndRestoresAssignmentToInProgress() {
    CompetitionPracticalAssignmentEntity assignment = assignment(501L, 1001L);
    assignment.setStatus("completed");
    assignment.setEndedAt(java.time.LocalDateTime.now());
    when(practicalAssignmentMapper.selectOne(any())).thenReturn(assignment);
    when(practicalAssignmentMapper.updateById(any(CompetitionPracticalAssignmentEntity.class)))
        .thenReturn(1);
    CompetitionPracticalScoreSheetEntity sheet = new CompetitionPracticalScoreSheetEntity();
    sheet.setId(800L);
    sheet.setAssignmentId(501L);
    sheet.setStatus("submitted_locked");
    when(scoreSheetMapper.selectOne(any())).thenReturn(sheet);
    when(executionService.isPracticalScoreCompleted(TENANT_ID, 501L)).thenReturn(false);

    service.returnForRescore(501L, "三裁判分差异常，退回复核");

    verify(executionService).unlockSheet(TENANT_ID, USER_ID, 800L, "三裁判分差异常，退回复核");
    assertThat(assignment.getStatus()).isEqualTo("in_progress");
    assertThat(assignment.getEndedAt()).isNull();
    assertThat(assignment.getUpdatedBy()).isEqualTo(USER_ID);
  }

  @Test
  void initializeRejectsStartedStage() {
    when(stageService.assertCanManage(STAGE_ID)).thenReturn(stage("in_progress"));

    assertThatThrownBy(() -> service.initialize(STAGE_ID, LEVEL_ID))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前赛段已开始或已结束，不能初始化正式比赛");
  }

  private void stubInitializeDependencies(CompetitionStageVO stage) {
    when(stageService.assertCanManage(STAGE_ID)).thenReturn(stage);

    CompetitionEventWorkTypeLevelEntity level = new CompetitionEventWorkTypeLevelEntity();
    level.setId(LEVEL_ID);
    level.setTenantId(TENANT_ID);
    level.setEventId(EVENT_ID);
    level.setEventWorkTypeId(WORK_TYPE_ID);
    level.setStagePlanStatus("confirmed");
    level.setDeleted(0);
    when(levelMapper.selectOne(any())).thenReturn(level);

    CompetitionStagePaperAssignmentEntity paper = new CompetitionStagePaperAssignmentEntity();
    paper.setTenantId(TENANT_ID);
    paper.setEventId(EVENT_ID);
    paper.setStageId(STAGE_ID);
    paper.setEventWorkTypeId(WORK_TYPE_ID);
    paper.setEventWorkTypeLevelId(LEVEL_ID);
    paper.setContestantMemberId(CONTESTANT_ID);
    paper.setContestantProfileId(600L);
    paper.setRegistrationId(700L);
    paper.setPaperSnapshotId(800L);
    paper.setAssessmentSheetId(900L);
    paper.setStatus("confirmed");
    paper.setDeleted(0);
    when(paperAssignmentMapper.selectByScopeForUpdate(
        TENANT_ID, EVENT_ID, STAGE_ID, WORK_TYPE_ID, LEVEL_ID, "practical"))
        .thenReturn(List.of(paper));

    when(refereeAssignmentMapper.selectList(any())).thenReturn(List.of(
        referee(1001L), referee(1002L), referee(1003L)));
    CompetitionRefereeGroupEntity configuredGroup = new CompetitionRefereeGroupEntity();
    configuredGroup.setId(100L);
    configuredGroup.setTenantId(TENANT_ID);
    configuredGroup.setLeaderRefereeMemberId(1001L);
    configuredGroup.setDeleted(0);
    when(refereeGroupMapper.selectById(100L)).thenReturn(configuredGroup);
    when(planMapper.selectOne(any())).thenReturn(null);
    when(planMapper.selectList(any())).thenReturn(List.of());
    when(planMapper.insert(any(CompetitionStageExecutionPlanEntity.class))).thenReturn(1);
    when(planMapper.updateById(any(CompetitionStageExecutionPlanEntity.class))).thenReturn(1);
    when(rosterSnapshotMapper.selectOne(any())).thenReturn(null);
    when(rosterSnapshotMapper.selectList(any())).thenReturn(List.of());
    when(rosterSnapshotMapper.insert(any(CompetitionStageRosterSnapshotEntity.class))).thenReturn(1);

    CompetitionStageRosterItemEntity rosterItem = new CompetitionStageRosterItemEntity();
    rosterItem.setId(1100L);
    rosterItem.setContestantMemberId(CONTESTANT_ID);
    rosterItem.setInclusionStatus("included");
    rosterItem.setDeleted(0);
    when(rosterItemMapper.selectList(any())).thenReturn(List.of(rosterItem));
    when(rosterItemMapper.insert(any(CompetitionStageRosterItemEntity.class))).thenReturn(1);

    CompetitionPracticalSessionEntity session = new CompetitionPracticalSessionEntity();
    session.setId(300L);
    session.setStatus("draft");
    when(executionService.createSession(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(session);
    when(executionService.publishSession(any(), any())).thenReturn(session);
    when(sessionMapper.updateById(any(CompetitionPracticalSessionEntity.class))).thenReturn(1);

    CompetitionPracticalJudgeGroupEntity group = new CompetitionPracticalJudgeGroupEntity();
    group.setId(400L);
    when(executionService.createGroup(any(), any(), any(), any(), any())).thenReturn(group);

    List<CompetitionPracticalAssignmentEntity> assignments = List.of(
        assignment(501L, 1001L), assignment(502L, 1002L), assignment(503L, 1003L));
    when(executionService.assign(any(), any(), any(), any())).thenReturn(assignments);
    when(sessionMapper.selectList(any())).thenReturn(List.of(session));
    when(practicalAssignmentMapper.selectList(any())).thenReturn(assignments);
    when(practicalAssignmentMapper.updateById(any(CompetitionPracticalAssignmentEntity.class)))
        .thenReturn(1);
    when(scoreSheetMapper.selectList(any())).thenReturn(List.of());
  }

  private CompetitionStageVO stage(String status) {
    CompetitionStageVO stage = new CompetitionStageVO();
    stage.setId(STAGE_ID);
    stage.setEventId(EVENT_ID);
    stage.setEventWorkTypeId(WORK_TYPE_ID);
    stage.setName("实操测试7");
    stage.setStatus(status);
    return stage;
  }

  private CompetitionRefereeAssignmentEntity referee(Long memberId) {
    CompetitionRefereeAssignmentEntity row = new CompetitionRefereeAssignmentEntity();
    row.setTenantId(TENANT_ID);
    row.setEventId(EVENT_ID);
    row.setStageId(STAGE_ID);
    row.setEventWorkTypeId(WORK_TYPE_ID);
    row.setEventWorkTypeLevelId(LEVEL_ID);
    row.setRefereeGroupId(100L);
    row.setRefereeMemberId(memberId);
    row.setContestantMemberId(CONTESTANT_ID);
    row.setStatus("active");
    row.setDeleted(0);
    return row;
  }

  private CompetitionPracticalAssignmentEntity assignment(Long id, Long refereeMemberId) {
    CompetitionPracticalAssignmentEntity row = new CompetitionPracticalAssignmentEntity();
    row.setId(id);
    row.setPlanId(200L);
    row.setSessionId(300L);
    row.setContestantMemberId(CONTESTANT_ID);
    row.setRefereeMemberId(refereeMemberId);
    row.setAssessmentSheetId(900L);
    row.setWorkstationCode("P-" + id);
    row.setStatus("assigned");
    return row;
  }
}
