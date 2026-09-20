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
import com.guoyu.platform.competition.entity.CompetitionEventMemberEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalAssignmentEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalJudgeGroupEntity;
import com.guoyu.platform.competition.entity.CompetitionPracticalScoreSheetEntity;
import com.guoyu.platform.competition.entity.CompetitionRefereeProfileEntity;
import com.guoyu.platform.competition.mapper.CompetitionEventMemberMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssessmentSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalJudgeGroupMapper;
import com.guoyu.platform.competition.mapper.CompetitionPracticalScoreSheetMapper;
import com.guoyu.platform.competition.mapper.CompetitionRefereeProfileMapper;
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
  private final CompetitionPracticalAssessmentSheetMapper assessmentSheetMapper =
      Mockito.mock(CompetitionPracticalAssessmentSheetMapper.class);
  private final CompetitionPracticalScoreSheetMapper scoreSheetMapper =
      Mockito.mock(CompetitionPracticalScoreSheetMapper.class);
  private final CompetitionPracticalAssessmentSheetService assessmentSheetService =
      Mockito.mock(CompetitionPracticalAssessmentSheetService.class);
  private final CompetitionPracticalExecutionService executionService =
      Mockito.mock(CompetitionPracticalExecutionService.class);

  private final CompetitionPracticalJudgeService service = new CompetitionPracticalJudgeService(
      profileMapper, eventMemberMapper, assignmentMapper, groupMapper, assessmentSheetMapper,
      scoreSheetMapper, assessmentSheetService, executionService);

  @BeforeEach
  void authenticate() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        USER_ID, TENANT_ID, "judge", "裁判", "platform",
        List.of("COMPETITION_PRACTICAL_REFEREE"),
        List.of("competition:practical-judge:score"), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));

    CompetitionRefereeProfileEntity profile = new CompetitionRefereeProfileEntity();
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
