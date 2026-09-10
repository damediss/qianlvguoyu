package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.exam.formal.FormalExamErrorCodes;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateQuestionScoreMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateResultMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageContentMapper;
import com.guoyu.platform.competition.mapper.CompetitionStagePaperAssignmentMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperQuestionMapper;
import com.guoyu.platform.competition.mapper.CompetitionTheoryPaperSnapshotMapper;
import com.guoyu.platform.competition.vo.CompetitionFormalCandidateExamVO;
import com.guoyu.platform.competition.vo.CompetitionStageVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CompetitionFormalExamForceSubmitServiceTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
  private static final Clock RUNNING_CLOCK =
      Clock.fixed(Instant.parse("2026-09-08T01:10:00Z"), ZONE);
  private static final Clock DEADLINE_CLOCK =
      Clock.fixed(Instant.parse("2026-09-08T02:30:04Z"), ZONE);
  private static final LocalDateTime RUNNING_NOW = LocalDateTime.ofInstant(
      Instant.parse("2026-09-08T01:10:00Z"), ZONE);
  private static final LocalDateTime DEADLINE = LocalDateTime.of(2026, 9, 8, 10, 30, 0);
  private static final LocalDateTime SCAN_TIME = LocalDateTime.of(2026, 9, 8, 10, 30, 4);

  private final CompetitionStageApplicationService stages =
      Mockito.mock(CompetitionStageApplicationService.class);
  private final CompetitionEventService events = Mockito.mock(CompetitionEventService.class);
  private final CompetitionEventWorkTypeMapper workTypes =
      Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levels =
      Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionFormalExamSessionMapper sessions =
      Mockito.mock(CompetitionFormalExamSessionMapper.class);
  private final CompetitionFormalCandidateExamMapper candidateExams =
      Mockito.mock(CompetitionFormalCandidateExamMapper.class);
  private final CompetitionFormalCandidateAnswerMapper answers =
      Mockito.mock(CompetitionFormalCandidateAnswerMapper.class);

  @BeforeEach
  void authenticate() {
    var principal = new PlatformUserPrincipal(
        1000L, 1L, "admin", "Admin", "platform", List.of(), List.of(), null);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    var stage = new CompetitionStageVO();
    stage.setId(10L);
    stage.setEventId(20L);
    stage.setEventWorkTypeId(30L);
    stage.setEventWorkTypeLevelCode("3");
    stage.setName("初赛");
    when(stages.assertCanManage(10L)).thenReturn(stage);
    var event = new CompetitionEventEntity();
    event.setId(20L);
    event.setName("技能大赛");
    when(events.getById(1L, 20L)).thenReturn(event);
    var workType = new CompetitionEventWorkTypeEntity();
    workType.setId(30L);
    workType.setDisplayName("电工");
    when(workTypes.selectOne(any())).thenReturn(workType);
    var level = new CompetitionEventWorkTypeLevelEntity();
    level.setId(40L);
    level.setLevelCode("3");
    level.setLevelName("高级工");
    when(levels.selectOne(any())).thenReturn(level);
    when(answers.countAnswered(9101L)).thenReturn(6);
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void runningBecomesForcedWithReasonAndOperator() {
    stubRunningSession();
    CompetitionFormalCandidateExamEntity running = candidate("running");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(running);
    when(candidateExams.markForcedIfRunning(
        eq(9101L), eq("running"), eq("forced_submitted"), eq(RUNNING_NOW),
        eq(FormalExamErrorCodes.SUBMIT_TYPE_FORCED), eq(6), eq("现场设备异常"), eq(1000L),
        eq(RUNNING_NOW), eq(1000L))).thenReturn(1);
    CompetitionFormalCandidateExamEntity forced = candidate("forced_submitted");
    forced.setSubmitTime(RUNNING_NOW);
    forced.setSubmitType(FormalExamErrorCodes.SUBMIT_TYPE_FORCED);
    forced.setForceSubmitReason("现场设备异常");
    forced.setForceSubmittedBy(1000L);
    forced.setAnsweredCount(6);
    when(candidateExams.selectById(9101L)).thenReturn(forced);

    CompetitionFormalCandidateExamVO vo = service(RUNNING_CLOCK)
        .forceSubmit(9001L, 9101L, "现场设备异常");

    assertThat(vo.status()).isEqualTo("forced_submitted");
    assertThat(vo.submitType()).isEqualTo("forced_submitted");
    assertThat(vo.submitTime()).isEqualTo(RUNNING_NOW);
    assertThat(vo.forceSubmitReason()).isEqualTo("现场设备异常");
    assertThat(vo.forceSubmittedBy()).isEqualTo("1000");
    assertThat(vo.answeredCount()).isEqualTo(6);
  }

  @Test
  void forcedSubmitIsIdempotentAndDoesNotOverwriteTrace() {
    stubRunningSession();
    LocalDateTime firstTime = RUNNING_NOW.minusMinutes(3);
    CompetitionFormalCandidateExamEntity forced = candidate("forced_submitted");
    forced.setSubmitTime(firstTime);
    forced.setSubmitType(FormalExamErrorCodes.SUBMIT_TYPE_FORCED);
    forced.setForceSubmitReason("原因A");
    forced.setForceSubmittedBy(1000L);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(forced);

    CompetitionFormalCandidateExamVO vo = service(RUNNING_CLOCK)
        .forceSubmit(9001L, 9101L, "另一原因");

    assertThat(vo.submitTime()).isEqualTo(firstTime);
    assertThat(vo.forceSubmitReason()).isEqualTo("原因A");
    assertThat(vo.forceSubmittedBy()).isEqualTo("1000");
    verify(candidateExams, never()).markForcedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any(), any(), any());
  }

  @Test
  void submittedIsNotOverwrittenByForce() {
    stubRunningSession();
    CompetitionFormalCandidateExamEntity submitted = candidate("submitted");
    submitted.setSubmitTime(RUNNING_NOW.minusMinutes(2));
    submitted.setSubmitType(FormalExamErrorCodes.SUBMIT_TYPE_MANUAL);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(submitted);

    CompetitionFormalCandidateExamVO vo = service(RUNNING_CLOCK)
        .forceSubmit(9001L, 9101L, "现场处理");

    assertThat(vo.status()).isEqualTo("submitted");
    verify(candidateExams, never()).markForcedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any(), any(), any());
  }

  @Test
  void autoSubmittedIsNotOverwrittenByForce() {
    stubRunningSession();
    CompetitionFormalCandidateExamEntity auto = candidate("auto_submitted");
    auto.setSubmitTime(DEADLINE);
    auto.setSubmitType(FormalExamErrorCodes.SUBMIT_TYPE_AUTO);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(auto);

    CompetitionFormalCandidateExamVO vo = service(RUNNING_CLOCK)
        .forceSubmit(9001L, 9101L, "现场处理");

    assertThat(vo.status()).isEqualTo("auto_submitted");
    verify(candidateExams, never()).markForcedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any(), any(), any());
  }

  @Test
  void waitingEnterCannotBeForceSubmitted() {
    stubRunningSession();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("waiting_enter"));

    assertThatThrownBy(() -> service(RUNNING_CLOCK).forceSubmit(9001L, 9101L, "缺考处理"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前选手尚未开始考试，不能强制交卷");
  }

  @Test
  void readyCannotBeForceSubmitted() {
    stubRunningSession();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("ready"));

    assertThatThrownBy(() -> service(RUNNING_CLOCK).forceSubmit(9001L, 9101L, "现场处理"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("选手考试状态异常，不能强制交卷");
  }

  @Test
  void absentCannotBeForceSubmitted() {
    stubRunningSession();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("absent"));

    assertThatThrownBy(() -> service(RUNNING_CLOCK).forceSubmit(9001L, 9101L, "缺考处理"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前选手已缺考，不能强制交卷");
  }

  @Test
  void forceAfterDeadlineBecomesAutoSubmittedNotForced() {
    stubRunningSession();
    CompetitionFormalCandidateExamEntity running = candidate("running");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(running);
    when(candidateExams.markSubmittedIfRunning(
        eq(9101L), eq("running"), eq("auto_submitted"), eq(DEADLINE),
        eq(FormalExamErrorCodes.SUBMIT_TYPE_AUTO), eq(6), eq(SCAN_TIME), isNull())).thenReturn(1);
    CompetitionFormalCandidateExamEntity auto = candidate("auto_submitted");
    auto.setSubmitTime(DEADLINE);
    auto.setSubmitType(FormalExamErrorCodes.SUBMIT_TYPE_AUTO);
    auto.setAnsweredCount(6);
    when(candidateExams.selectById(9101L)).thenReturn(auto);

    CompetitionFormalCandidateExamVO vo = service(DEADLINE_CLOCK)
        .forceSubmit(9001L, 9101L, "现场设备异常");

    assertThat(vo.status()).isEqualTo("auto_submitted");
    assertThat(vo.submitTime()).isEqualTo(DEADLINE);
    verify(candidateExams, never()).markForcedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any(), any(), any());
  }

  @Test
  void rejectsCandidateFromAnotherSession() {
    stubRunningSession();
    CompetitionFormalCandidateExamEntity other = candidate("running");
    other.setExamSessionId(9999L);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(other);

    assertThatThrownBy(() -> service(RUNNING_CLOCK).forceSubmit(9001L, 9101L, "现场处理"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("选手考试实例不存在");
  }

  private CompetitionFormalExamSessionApplicationService service(Clock clock) {
    return new CompetitionFormalExamSessionApplicationService(
        stages, events, workTypes, levels,
        Mockito.mock(CompetitionStageContentMapper.class),
        Mockito.mock(CompetitionStagePaperAssignmentMapper.class),
        Mockito.mock(CompetitionTheoryPaperSnapshotMapper.class),
        Mockito.mock(CompetitionTheoryPaperQuestionMapper.class),
        sessions,
        Mockito.mock(CompetitionFormalExamPaperSnapshotMapper.class),
        Mockito.mock(CompetitionFormalExamQuestionSnapshotMapper.class),
        candidateExams,
        Mockito.mock(CompetitionFormalCandidateResultMapper.class),
        Mockito.mock(CompetitionFormalCandidateQuestionScoreMapper.class),
        clock,
        new CompetitionFormalExamCandidateFinalizationService(candidateExams, answers));
  }

  private void stubRunningSession() {
    CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
    session.setId(9001L);
    session.setTenantId(1L);
    session.setEventId(20L);
    session.setStageId(10L);
    session.setStatus("running");
    session.setDurationSeconds(5400);
    session.setActualStartTime(LocalDateTime.of(2026, 9, 8, 9, 0, 0));
    when(sessions.selectById(9001L)).thenReturn(session);
  }

  private static CompetitionFormalCandidateExamEntity candidate(String status) {
    CompetitionFormalCandidateExamEntity entity = new CompetitionFormalCandidateExamEntity();
    entity.setId(9101L);
    entity.setTenantId(1L);
    entity.setExamSessionId(9001L);
    entity.setCandidateNo("001");
    entity.setCandidateName("张三");
    entity.setStatus(status);
    entity.setAnsweredCount(6);
    entity.setTotalQuestionCount(10);
    return entity;
  }
}
