package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreEntryReq;
import com.guoyu.platform.competition.dto.CompetitionPracticalScoreSaveReq;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalJudgeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeGroupMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssessmentSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalJudgeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeGroupMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.mapper.CompetitionRosterItemMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionPracticalJudgeServiceTest {
  private static final Long TENANT_ID = 10159L;
  private static final Long USER_ID = 88L;
  private static final Long LEADER_MEMBER_ID = 1001L;

  private final CompetitionRefereeProfileMapper profileMapper =
      Mockito.mock(CompetitionRefereeProfileMapper.class);
  private final CompetitionEventMemberMapper eventMemberMapper =
      Mockito.mock(CompetitionEventMemberMapper.class);
  private final CompetitionPracticalAssignmentMapper assignmentMapper =
      Mockito.mock(CompetitionPracticalAssignmentMapper.class);
  private final CompetitionPracticalJudgeGroupMapper groupMapper =
      Mockito.mock(CompetitionPracticalJudgeGroupMapper.class);
  private final CompetitionRefereeGroupMapper configuredGroupMapper =
      Mockito.mock(CompetitionRefereeGroupMapper.class);
  private final CompetitionRefereeGroupMemberMapper configuredGroupMemberMapper =
      Mockito.mock(CompetitionRefereeGroupMemberMapper.class);
  private final CompetitionPracticalAssessmentSheetMapper assessmentSheetMapper =
      Mockito.mock(CompetitionPracticalAssessmentSheetMapper.class);
  private final CompetitionPracticalScoreSheetMapper scoreSheetMapper =
      Mockito.mock(CompetitionPracticalScoreSheetMapper.class);
  private final CompetitionPracticalSessionMapper sessionMapper =
      Mockito.mock(CompetitionPracticalSessionMapper.class);
  private final CompetitionStageMapper stageMapper =
      Mockito.mock(CompetitionStageMapper.class);
  private final CompetitionEventMapper eventMapper =
      Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionEventWorkTypeMapper eventWorkTypeMapper =
      Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper eventWorkTypeLevelMapper =
      Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionContestantProfileMapper contestantProfileMapper =
      Mockito.mock(CompetitionContestantProfileMapper.class);
  private final CompetitionRegistrationMapper registrationMapper =
      Mockito.mock(CompetitionRegistrationMapper.class);
  private final CompetitionRosterItemMapper rosterItemMapper =
      Mockito.mock(CompetitionRosterItemMapper.class);
  private final CompetitionPracticalAssessmentSheetService assessmentSheetService =
      Mockito.mock(CompetitionPracticalAssessmentSheetService.class);
  private final CompetitionPracticalExecutionService executionService =
      Mockito.mock(CompetitionPracticalExecutionService.class);

  private final CompetitionPracticalJudgeService service = new CompetitionPracticalJudgeService(
      profileMapper, eventMemberMapper, assignmentMapper, groupMapper, configuredGroupMapper,
      configuredGroupMemberMapper, assessmentSheetMapper, scoreSheetMapper, sessionMapper,
      stageMapper, eventMapper, eventWorkTypeMapper, eventWorkTypeLevelMapper,
      contestantProfileMapper, registrationMapper, rosterItemMapper, assessmentSheetService,
      executionService);

  @BeforeEach
  void authenticate() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        USER_ID, TENANT_ID, "judge", "裁判", "platform",
        List.of("COMPETITION_PRACTICAL_REFEREE"),
        List.of("competition:practical-judge:score"), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));

    CompetitionRefereeProfileEntity profile = new CompetitionRefereeProfileEntity();
    profile.setId(11L);
    profile.setTenantId(TENANT_ID);
    profile.setPersonId(11L);
    profile.setUserId(USER_ID);
    profile.setStatus("active");
    profile.setDeleted(0);
    when(profileMapper.selectOne(any())).thenReturn(profile);

    CompetitionEventMemberEntity member = new CompetitionEventMemberEntity();
    member.setId(LEADER_MEMBER_ID);
    member.setTenantId(TENANT_ID);
    member.setPersonId(11L);
    member.setRoleType("referee");
    member.setStatus("active");
    member.setDeleted(0);
    when(eventMemberMapper.selectList(any())).thenReturn(List.of(member));
    when(assignmentMapper.updateById(any(CompetitionPracticalAssignmentEntity.class))).thenReturn(1);
    when(executionService.previewPracticalScoreDifference(any(), any(), any(), any()))
        .thenReturn(new CompetitionPracticalExecutionService.PracticalScoreDifferencePreview(
            null, null, 0, "waiting", false));
    when(executionService.previewPracticalScoreDifference(any(), any()))
        .thenReturn(new CompetitionPracticalExecutionService.PracticalScoreDifferencePreview(
            null, null, 0, "waiting", false));
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void thirdDeviceConfirmationMakesSessionReady() {
    CompetitionPracticalAssignmentEntity current = assignment(501L, LEADER_MEMBER_ID, false);
    List<CompetitionPracticalAssignmentEntity> assignments = List.of(
        current, assignment(502L, 1002L, true), assignment(503L, 1003L, true));
    givenOwnedTask(current, assignments, LEADER_MEMBER_ID);

    var result = service.confirmStart(current.getId());

    assertThat(current.getStartConfirmedAt()).isNotNull();
    assertThat(result.startStatus()).isEqualTo("ready");
    assertThat(result.confirmedJudges()).isEqualTo(3);
    assertThat(result.currentJudgeConfirmed()).isTrue();
    verify(assignmentMapper).updateById(current);
  }

  @Test
  void leaderStartsAllThreeAssignmentsOnlyAfterEveryDeviceConfirmed() {
    CompetitionPracticalAssignmentEntity current = assignment(501L, LEADER_MEMBER_ID, true);
    List<CompetitionPracticalAssignmentEntity> assignments = List.of(
        current, assignment(502L, 1002L, true), assignment(503L, 1003L, true));
    givenOwnedTask(current, assignments, LEADER_MEMBER_ID);

    var result = service.startSession(current.getId());

    verify(executionService).ensureInteractiveStage(TENANT_ID, USER_ID, current.getSessionId());
    assertThat(result.startStatus()).isEqualTo("started");
    assertThat(assignments).allSatisfy(item -> {
      assertThat(item.getStatus()).isEqualTo("in_progress");
      assertThat(item.getStartedAt()).isNotNull();
      assertThat(item.getStartedBy()).isEqualTo(USER_ID);
    });
    verify(assignmentMapper, Mockito.times(3))
        .updateById(any(CompetitionPracticalAssignmentEntity.class));
  }

  @Test
  void leaderStartStopsWhenStageCannotEnterProgress() {
    CompetitionPracticalAssignmentEntity current = assignment(501L, LEADER_MEMBER_ID, true);
    List<CompetitionPracticalAssignmentEntity> assignments = List.of(
        current, assignment(502L, 1002L, true), assignment(503L, 1003L, true));
    givenOwnedTask(current, assignments, LEADER_MEMBER_ID);
    doThrow(new BusinessException(ErrorCode.CONFLICT, "当前赛段不允许执行交互式操作"))
        .when(executionService).ensureInteractiveStage(any(), any(), any());

    assertThatThrownBy(() -> service.startSession(current.getId()))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前赛段不允许执行交互式操作");
    verify(assignmentMapper, never())
        .updateById(any(CompetitionPracticalAssignmentEntity.class));
  }

  @Test
  void groupMemberCannotStartSession() {
    CompetitionPracticalAssignmentEntity current = assignment(502L, LEADER_MEMBER_ID, true);
    current.setRefereeMemberId(1002L);
    CompetitionEventMemberEntity member = new CompetitionEventMemberEntity();
    member.setId(1002L);
    when(eventMemberMapper.selectList(any())).thenReturn(List.of(member));
    givenOwnedTask(current, List.of(current, assignment(501L, LEADER_MEMBER_ID, true),
        assignment(503L, 1003L, true)), LEADER_MEMBER_ID);

    assertThatThrownBy(() -> service.startSession(current.getId()))
        .isInstanceOf(BusinessException.class)
        .hasMessage("仅裁判组组长可以开始考试");
    verify(assignmentMapper, never())
        .updateById(any(CompetitionPracticalAssignmentEntity.class));
  }

  @Test
  void scoringRemainsLockedBeforeLeaderStarts() {
    CompetitionPracticalAssignmentEntity current = assignment(501L, LEADER_MEMBER_ID, true);
    givenOwnedTask(current, List.of(current), LEADER_MEMBER_ID);

    assertThatThrownBy(() -> service.open(current.getId()))
        .isInstanceOf(BusinessException.class)
        .hasMessage("组长尚未统一开考，当前不能评分");
    verify(executionService, never()).openSheet(any(), any(), any());
  }

  @Test
  void submitQuestionDoesNotCompleteAssignmentUntilAllQuestionsLocked() {
    CompetitionPracticalAssignmentEntity current = startedAssignment();
    givenOwnedTask(current, List.of(current), LEADER_MEMBER_ID);
    CompetitionPracticalScoreSheetEntity sheet = draftSheet();
    when(scoreSheetMapper.selectOne(any())).thenReturn(sheet);
    when(executionService.submitQuestions(any(), any(), any(), any())).thenReturn(sheet);
    when(executionService.entries(any(), any())).thenReturn(List.of());

    var result = service.save(current.getId(), scoreReq(), true);

    assertThat(current.getStatus()).isEqualTo("in_progress");
    assertThat(result.scoreSheetStatus()).isEqualTo("draft");
    verify(assignmentMapper, never()).updateById(current);
  }

  @Test
  void lastQuestionSubmitLocksAssignment() {
    CompetitionPracticalAssignmentEntity current = startedAssignment();
    givenOwnedTask(current, List.of(current), LEADER_MEMBER_ID);
    CompetitionPracticalScoreSheetEntity sheet = draftSheet();
    when(scoreSheetMapper.selectOne(any())).thenReturn(sheet);
    when(executionService.submitQuestions(any(), any(), any(), any())).thenAnswer(invocation -> {
      sheet.setStatus("submitted_locked");
      return sheet;
    });
    when(executionService.entries(any(), any())).thenReturn(List.of());

    var result = service.save(current.getId(), scoreReq(), true);

    assertThat(current.getStatus()).isEqualTo("completed");
    assertThat(result.scoreSheetStatus()).isEqualTo("submitted_locked");
    verify(assignmentMapper).updateById(current);
  }

  @Test
  void leaderCompletesExamOnlyAfterAllJudgesSubmit() {
    CompetitionPracticalAssignmentEntity current = startedAssignment();
    current.setStatus("completed");
    CompetitionPracticalAssignmentEntity memberA = assignment(502L, 1002L, true);
    memberA.setStatus("completed");
    CompetitionPracticalAssignmentEntity memberB = assignment(503L, 1003L, true);
    memberB.setStatus("completed");
    givenOwnedTask(current, List.of(current, memberA, memberB), LEADER_MEMBER_ID);
    when(executionService.isPracticalScoreCompleted(TENANT_ID, current.getId()))
        .thenReturn(true);
    when(executionService.entries(any(), any())).thenReturn(List.of());

    var result = service.completeSession(current.getId());

    verify(executionService).completePracticalScore(TENANT_ID, USER_ID, current.getId());
    assertThat(result.startStatus()).isEqualTo("completed");
  }

  @Test
  void groupMemberCannotCompleteExam() {
    CompetitionPracticalAssignmentEntity current = assignment(502L, 1002L, true);
    current.setStatus("completed");
    CompetitionEventMemberEntity member = new CompetitionEventMemberEntity();
    member.setId(1002L);
    when(eventMemberMapper.selectList(any())).thenReturn(List.of(member));
    givenOwnedTask(current, List.of(current), LEADER_MEMBER_ID);

    assertThatThrownBy(() -> service.completeSession(current.getId()))
        .isInstanceOf(BusinessException.class)
        .hasMessage("仅裁判组组长可以完成考试并提交实操成绩");
    verify(executionService, never()).completePracticalScore(any(), any(), any());
  }

  @Test
  void leaderPersistsActiveStageForTheWholeRefereeGroup() {
    CompetitionPracticalAssignmentEntity preliminary = assignment(501L, LEADER_MEMBER_ID, true);
    CompetitionPracticalAssignmentEntity finalTask = assignment(601L, LEADER_MEMBER_ID, true);
    finalTask.setSessionId(301L);
    finalTask.setContestantMemberId(701L);
    givenConfiguredGroup(true, 10L);
    givenTaskDisplays(List.of(preliminary, finalTask));

    var result = service.selectActiveStage("11");

    assertThat(result).extracting(item -> item.activeStageId()).containsOnly("11");
    assertThat(result).extracting(item -> item.stageId()).containsExactlyInAnyOrder("10", "11");
    verify(configuredGroupMapper).updateById(any(CompetitionRefereeGroupEntity.class));
  }

  @Test
  void groupMemberCannotSwitchActiveStage() {
    givenConfiguredGroup(false, 10L);
    givenTaskDisplays(List.of(assignment(501L, 1002L, true)));

    assertThatThrownBy(() -> service.selectActiveStage("11"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("仅裁判组组长可以切换赛段任务");
    verify(configuredGroupMapper, never()).updateById(any(CompetitionRefereeGroupEntity.class));
  }

  @Test
  void memberTaskListOnlyIncludesLeaderSelectedStage() {
    CompetitionEventMemberEntity member = new CompetitionEventMemberEntity();
    member.setId(1002L);
    member.setTenantId(TENANT_ID);
    member.setPersonId(11L);
    member.setRoleType("referee");
    member.setStatus("active");
    member.setDeleted(0);
    when(eventMemberMapper.selectList(any())).thenReturn(List.of(member));

    CompetitionPracticalAssignmentEntity preliminary = assignment(501L, 1002L, true);
    CompetitionPracticalAssignmentEntity finalTask = assignment(601L, 1002L, true);
    finalTask.setSessionId(301L);
    finalTask.setContestantMemberId(701L);
    givenConfiguredGroup(false, 10L);
    givenTaskDisplays(List.of(preliminary, finalTask));

    var result = service.tasks();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).stageId()).isEqualTo("10");
    assertThat(result.get(0).stageName()).isEqualTo("初赛");
    assertThat(result.get(0).eventName()).isEqualTo("技能大赛");
    assertThat(result.get(0).workTypeName()).isEqualTo("电工");
    assertThat(result.get(0).levelName()).isEqualTo("高级工");
    assertThat(result.get(0).activeStageId()).isEqualTo("10");
    assertThat(result.get(0).judgeRole()).isEqualTo("member");
  }

  private void givenOwnedTask(
      CompetitionPracticalAssignmentEntity current,
      List<CompetitionPracticalAssignmentEntity> assignments,
      Long leaderMemberId) {
    when(assignmentMapper.selectById(current.getId())).thenReturn(current);
    when(assignmentMapper.selectList(any())).thenReturn(assignments);
    CompetitionPracticalJudgeGroupEntity group = new CompetitionPracticalJudgeGroupEntity();
    group.setId(400L);
    group.setTenantId(TENANT_ID);
    group.setLeaderRefereeMemberId(leaderMemberId);
    group.setStatus("published");
    group.setDeleted(0);
    when(groupMapper.selectById(400L)).thenReturn(group);
    when(groupMapper.selectOne(any())).thenReturn(group);
  }

  private void givenConfiguredGroup(boolean leader, Long activeStageId) {
    CompetitionRefereeGroupMemberEntity membership = new CompetitionRefereeGroupMemberEntity();
    membership.setId(81L);
    membership.setTenantId(TENANT_ID);
    membership.setGroupId(800L);
    membership.setRefereeProfileId(11L);
    membership.setMemberRole(leader ? "leader" : "member");
    membership.setStatus("active");
    membership.setDeleted(0);
    when(configuredGroupMemberMapper.selectOne(any())).thenReturn(membership);

    CompetitionRefereeGroupEntity group = new CompetitionRefereeGroupEntity();
    group.setId(800L);
    group.setTenantId(TENANT_ID);
    group.setGroupName("第一裁判小组");
    group.setStatus("confirmed");
    group.setLeaderRefereeProfileId(leader ? 11L : 99L);
    group.setLeaderRefereeMemberId(leader ? LEADER_MEMBER_ID : 1099L);
    group.setActiveStageId(activeStageId);
    group.setDeleted(0);
    when(configuredGroupMapper.selectById(800L)).thenReturn(group);
    when(configuredGroupMapper.selectOne(any())).thenReturn(group);
    when(configuredGroupMapper.updateById(any(CompetitionRefereeGroupEntity.class))).thenReturn(1);
  }

  private void givenTaskDisplays(List<CompetitionPracticalAssignmentEntity> assignments) {
    when(assignmentMapper.selectList(any())).thenReturn(assignments);
    CompetitionPracticalJudgeGroupEntity runtimeGroup = new CompetitionPracticalJudgeGroupEntity();
    runtimeGroup.setId(400L);
    runtimeGroup.setTenantId(TENANT_ID);
    runtimeGroup.setLeaderRefereeMemberId(LEADER_MEMBER_ID);
    runtimeGroup.setStatus("published");
    runtimeGroup.setDeleted(0);
    when(groupMapper.selectById(400L)).thenReturn(runtimeGroup);

    CompetitionPracticalSessionEntity preliminary = session(300L, 10L);
    CompetitionPracticalSessionEntity finals = session(301L, 11L);
    when(sessionMapper.selectList(any())).thenReturn(List.of(preliminary, finals));
    when(stageMapper.selectList(any())).thenReturn(List.of(stage(10L, "初赛", 1), stage(11L, "决赛", 2)));
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(20L);
    event.setTenantId(TENANT_ID);
    event.setName("技能大赛");
    event.setDeleted(0);
    when(eventMapper.selectList(any())).thenReturn(List.of(event));
    CompetitionEventWorkTypeEntity workType = new CompetitionEventWorkTypeEntity();
    workType.setId(30L);
    workType.setTenantId(TENANT_ID);
    workType.setDisplayName("电工");
    workType.setDeleted(0);
    when(eventWorkTypeMapper.selectList(any())).thenReturn(List.of(workType));
    CompetitionEventWorkTypeLevelEntity level = new CompetitionEventWorkTypeLevelEntity();
    level.setId(40L);
    level.setTenantId(TENANT_ID);
    level.setLevelCode("3");
    level.setLevelName("高级工");
    level.setDeleted(0);
    when(eventWorkTypeLevelMapper.selectList(any())).thenReturn(List.of(level));
  }

  private CompetitionPracticalSessionEntity session(Long id, Long stageId) {
    CompetitionPracticalSessionEntity row = new CompetitionPracticalSessionEntity();
    row.setId(id);
    row.setTenantId(TENANT_ID);
    row.setStageId(stageId);
    row.setEventId(20L);
    row.setEventWorkTypeId(30L);
    row.setEventWorkTypeLevelId(40L);
    row.setDeleted(0);
    return row;
  }

  private CompetitionStageEntity stage(Long id, String name, int sortOrder) {
    CompetitionStageEntity row = new CompetitionStageEntity();
    row.setId(id);
    row.setTenantId(TENANT_ID);
    row.setName(name);
    row.setSortOrder(sortOrder);
    row.setDeleted(0);
    return row;
  }

  private CompetitionPracticalAssignmentEntity startedAssignment() {
    CompetitionPracticalAssignmentEntity row = assignment(501L, LEADER_MEMBER_ID, true);
    row.setStatus("in_progress");
    return row;
  }

  private CompetitionPracticalScoreSheetEntity draftSheet() {
    CompetitionPracticalScoreSheetEntity sheet = new CompetitionPracticalScoreSheetEntity();
    sheet.setId(901L);
    sheet.setAssignmentId(501L);
    sheet.setStatus("draft");
    sheet.setDeleted(0);
    return sheet;
  }

  private CompetitionPracticalScoreSaveReq scoreReq() {
    return new CompetitionPracticalScoreSaveReq(List.of(
        new CompetitionPracticalScoreEntryReq(null, 7001L, 1,
            new java.math.BigDecimal("19.50"), new java.math.BigDecimal("0.50"), null)));
  }

  private CompetitionPracticalAssignmentEntity assignment(
      Long id, Long refereeMemberId, boolean confirmed) {
    CompetitionPracticalAssignmentEntity row = new CompetitionPracticalAssignmentEntity();
    row.setId(id);
    row.setTenantId(TENANT_ID);
    row.setPlanId(200L);
    row.setSessionId(300L);
    row.setJudgeGroupId(400L);
    row.setRosterItemId(600L);
    row.setContestantMemberId(700L);
    row.setRefereeMemberId(refereeMemberId);
    row.setAssessmentSheetId(null);
    row.setWorkstationCode("P-01");
    row.setStatus("assigned");
    row.setAssignedAt(LocalDateTime.now());
    row.setStartConfirmedAt(confirmed ? LocalDateTime.now() : null);
    row.setStartConfirmedBy(confirmed ? USER_ID : null);
    row.setDeleted(0);
    return row;
  }
}
