package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpLogic;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerAnswerSaveReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerAnswerVersionItem;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerSubmitReq;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection;
import com.guoyu.platform.competition.exam.formal.FormalExamErrorCodes;
import com.guoyu.platform.competition.exam.security.CompetitionExamPrincipal;
import com.guoyu.platform.competition.mapper.CompetitionContestantProfileMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeLevelMapper;
import com.guoyu.platform.competition.mapper.CompetitionEventWorkTypeMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateAnswerMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamPaperSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamQuestionSnapshotMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import com.guoyu.platform.competition.mapper.CompetitionStageMapper;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerDataHash;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.competition.vo.CompetitionFormalExamAnswerVersionMismatchVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerSubmitVO;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

class CompetitionFormalExamPlayerSubmitServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-08T01:26:18Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Shanghai"));
  private static final LocalDateTime SERVER_NOW = LocalDateTime.ofInstant(NOW, CLOCK.getZone());

  private final CompetitionFormalExamSessionMapper sessions =
      Mockito.mock(CompetitionFormalExamSessionMapper.class);
  private final CompetitionFormalCandidateExamMapper candidateExams =
      Mockito.mock(CompetitionFormalCandidateExamMapper.class);
  private final CompetitionContestantProfileMapper profiles =
      Mockito.mock(CompetitionContestantProfileMapper.class);
  private final CompetitionEventService events = Mockito.mock(CompetitionEventService.class);
  private final CompetitionStageMapper stages = Mockito.mock(CompetitionStageMapper.class);
  private final CompetitionEventWorkTypeMapper workTypes =
      Mockito.mock(CompetitionEventWorkTypeMapper.class);
  private final CompetitionEventWorkTypeLevelMapper levels =
      Mockito.mock(CompetitionEventWorkTypeLevelMapper.class);
  private final CompetitionFormalExamPaperSnapshotMapper papers =
      Mockito.mock(CompetitionFormalExamPaperSnapshotMapper.class);
  private final CompetitionFormalExamQuestionSnapshotMapper questions =
      Mockito.mock(CompetitionFormalExamQuestionSnapshotMapper.class);
  private final CompetitionFormalCandidateAnswerMapper answers =
      Mockito.mock(CompetitionFormalCandidateAnswerMapper.class);
  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  private final FormalExamCandidateQuestionProjection projection =
      new FormalExamCandidateQuestionProjection(objectMapper);
  private final CompetitionFormalScoringTaskService scoringTasks =
      Mockito.mock(CompetitionFormalScoringTaskService.class);

  private final CompetitionFormalExamPlayerApplicationService service =
      new CompetitionFormalExamPlayerApplicationService(
          Mockito.mock(CompetitionPlayerIdentityMapper.class),
          Mockito.mock(CompetitionPlayerIdentityDomainService.class),
          Mockito.mock(CompetitionPlayerDataHash.class),
          Mockito.mock(PasswordEncoder.class),
          Mockito.mock(StpLogic.class),
          sessions, candidateExams, profiles, events, stages, workTypes, levels,
          papers, questions, answers, projection, CLOCK, null);

  @BeforeEach
  void authenticatePlayer() {
    service.setScoringTaskService(scoringTasks);
    CompetitionExamPrincipal principal = new CompetitionExamPrincipal(77L, 1L);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void submitLocksCandidateThenMarksSubmittedOnce() {
    stubRunningPaper();
    when(answers.selectByCandidateExamId(9101L)).thenReturn(List.of(
        storedAnswer(9301L, 2, 1), storedAnswer(9302L, 1, 1)));
    when(answers.countAnswered(9101L)).thenReturn(2);
    when(candidateExams.markSubmittedIfRunning(
        eq(9101L), eq("running"), eq("submitted"), eq(SERVER_NOW), eq(FormalExamErrorCodes.SUBMIT_TYPE_MANUAL),
        eq(2), eq(SERVER_NOW), eq(77L))).thenReturn(1);
    CompetitionFormalCandidateExamEntity submitted = candidate("submitted");
    submitted.setSubmitTime(SERVER_NOW);
    submitted.setSubmitType(FormalExamErrorCodes.SUBMIT_TYPE_MANUAL);
    submitted.setAnsweredCount(2);
    when(candidateExams.selectById(9101L)).thenReturn(submitted);

    CompetitionFormalExamPlayerSubmitVO vo = service.submit("9001", allAnsweredReq(false));

    assertThat(vo.candidateExamStatus()).isEqualTo("submitted");
    assertThat(vo.submitTime()).isEqualTo(SERVER_NOW);
    assertThat(vo.answeredCount()).isEqualTo(2);
    assertThat(vo.totalQuestionCount()).isEqualTo(2);
    assertThat(vo.unansweredCount()).isZero();
    assertThat(vo.serverTime()).isEqualTo(SERVER_NOW);
    InOrder order = inOrder(candidateExams, answers);
    order.verify(candidateExams).selectByIdForUpdate(9101L);
    order.verify(answers).selectByCandidateExamId(9101L);
    order.verify(candidateExams).markSubmittedIfRunning(
        eq(9101L), eq("running"), eq("submitted"), eq(SERVER_NOW), eq(FormalExamErrorCodes.SUBMIT_TYPE_MANUAL),
        eq(2), eq(SERVER_NOW), eq(77L));
    verify(sessions, never()).selectByIdForUpdate(any());
    verify(scoringTasks).enqueueInitial(submitted, "manual-submit", 77L);
  }

  @Test
  void unansweredWithoutConfirmIsRejected() {
    stubRunningPaper();
    when(answers.selectByCandidateExamId(9101L)).thenReturn(List.of(storedAnswer(9301L, 2, 1)));
    when(answers.countAnswered(9101L)).thenReturn(1);

    assertThatThrownBy(() -> service.submit("9001", versionsReq(false,
        version("9301", 2), version("9302", 0))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前还有未作答题目，请确认后再交卷")
        .extracting("code")
        .isEqualTo(FormalExamErrorCodes.UNANSWERED_CONFIRM_REQUIRED);
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  void unansweredWithConfirmSubmits() throws Exception {
    stubRunningPaper();
    when(answers.selectByCandidateExamId(9101L)).thenReturn(List.of(storedAnswer(9301L, 2, 1)));
    when(answers.countAnswered(9101L)).thenReturn(1);
    when(candidateExams.markSubmittedIfRunning(
        eq(9101L), eq("running"), eq("submitted"), eq(SERVER_NOW), eq(FormalExamErrorCodes.SUBMIT_TYPE_MANUAL),
        eq(1), eq(SERVER_NOW), eq(77L))).thenReturn(1);
    CompetitionFormalCandidateExamEntity submitted = candidate("submitted");
    submitted.setSubmitTime(SERVER_NOW);
    submitted.setSubmitType(FormalExamErrorCodes.SUBMIT_TYPE_MANUAL);
    when(candidateExams.selectById(9101L)).thenReturn(submitted);

    CompetitionFormalExamPlayerSubmitVO vo = service.submit("9001", versionsReq(true,
        version("9301", 2), version("9302", 0)));

    assertThat(vo.candidateExamStatus()).isEqualTo("submitted");
    assertThat(vo.answeredCount()).isEqualTo(1);
    assertThat(vo.unansweredCount()).isEqualTo(1);
    assertThat(vo.submitTime()).isEqualTo(SERVER_NOW);
    String json = new ObjectMapper().registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .writeValueAsString(vo);
    assertThat(json)
        .doesNotContain("correctAnswersJson")
        .doesNotContain("fillAnswer")
        .doesNotContain("isCorrect")
        .doesNotContain("awardedScore")
        .doesNotContain("resultStatus")
        .doesNotContain("questionScores")
        .doesNotContain("totalScore");
  }

  @Test
  void versionMismatchKeepsRunning() {
    stubRunningPaper();
    when(answers.selectByCandidateExamId(9101L)).thenReturn(List.of(
        storedAnswer(9301L, 4, 1), storedAnswer(9302L, 1, 1)));

    assertThatThrownBy(() -> service.submit("9001", versionsReq(true,
        version("9301", 5), version("9302", 1))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("部分答案尚未完成服务器同步")
        .satisfies(error -> {
          BusinessException ex = (BusinessException) error;
          assertThat(ex.getCode()).isEqualTo(FormalExamErrorCodes.ANSWERS_NOT_SYNCED);
          @SuppressWarnings("unchecked")
          Map<String, Object> data = (Map<String, Object>) ex.getData();
          @SuppressWarnings("unchecked")
          List<CompetitionFormalExamAnswerVersionMismatchVO> questions =
              (List<CompetitionFormalExamAnswerVersionMismatchVO>) data.get("questions");
          assertThat(questions).hasSize(1);
          assertThat(questions.getFirst().questionSnapshotId()).isEqualTo("9301");
          assertThat(questions.getFirst().clientVersion()).isEqualTo(5);
          assertThat(questions.getFirst().serverVersion()).isEqualTo(4);
        });
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  void missingQuestionIsRejected() {
    stubRunningPaper();
    assertThatThrownBy(() -> service.submit("9001", versionsReq(true, version("9301", 2))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("交卷答案版本清单与当前试卷不一致");
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  void duplicateQuestionIsRejected() {
    stubRunningPaper();
    assertThatThrownBy(() -> service.submit("9001", versionsReq(true,
        version("9301", 2), version("9302", 1), version("9301", 2))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("交卷答案版本清单存在重复题目");
  }

  @Test
  void foreignQuestionIsRejected() {
    stubRunningPaper();
    assertThatThrownBy(() -> service.submit("9001", versionsReq(true,
        version("9301", 2), version("9401", 1))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("交卷答案版本清单与当前试卷不一致");
  }

  @Test
  void unknownQuestionIsRejected() {
    stubRunningPaper();
    assertThatThrownBy(() -> service.submit("9001", versionsReq(true,
        version("9301", 2), version("9999", 1))))
        .isInstanceOf(BusinessException.class)
        .hasMessage("交卷答案版本清单与当前试卷不一致");
  }

  @Test
  void repeatSubmitIsIdempotent() {
    stubMapped("submitted", "running");
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusMinutes(5));
    when(sessions.selectById(9001L)).thenReturn(session);
    CompetitionFormalCandidateExamEntity submitted = candidate("submitted");
    LocalDateTime firstSubmit = SERVER_NOW.minusMinutes(3);
    submitted.setSubmitTime(firstSubmit);
    submitted.setSubmitType(FormalExamErrorCodes.SUBMIT_TYPE_MANUAL);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(submitted);
    when(questions.selectByPaperSnapshotId(9201L)).thenReturn(List.of(
        question(9301L, 1), question(9302L, 2)));
    when(answers.countAnswered(9101L)).thenReturn(2);

    CompetitionFormalExamPlayerSubmitVO first = service.submit("9001", allAnsweredReq(true));
    CompetitionFormalExamPlayerSubmitVO second = service.submit("9001", allAnsweredReq(true));

    assertThat(first.submitTime()).isEqualTo(firstSubmit);
    assertThat(second.submitTime()).isEqualTo(firstSubmit);
    assertThat(first.candidateExamStatus()).isEqualTo("submitted");
    assertThat(second.candidateExamStatus()).isEqualTo("submitted");
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  void autoSubmittedIsNotConvertedToSubmitted() {
    stubMapped("auto_submitted", "running");
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusMinutes(5));
    when(sessions.selectById(9001L)).thenReturn(session);
    CompetitionFormalCandidateExamEntity terminal = candidate("auto_submitted");
    terminal.setSubmitTime(SERVER_NOW.minusMinutes(1));
    terminal.setSubmitType("auto");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(terminal);
    when(questions.selectByPaperSnapshotId(9201L)).thenReturn(List.of(
        question(9301L, 1), question(9302L, 2)));
    when(answers.countAnswered(9101L)).thenReturn(2);

    CompetitionFormalExamPlayerSubmitVO vo = service.submit("9001", allAnsweredReq(true));

    assertThat(vo.candidateExamStatus()).isEqualTo("auto_submitted");
    assertThat(vo.submitTime()).isEqualTo(SERVER_NOW.minusMinutes(1));
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  void absentCannotBeConvertedToSubmitted() {
    stubMapped("absent", "finished");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("absent"));

    assertThatThrownBy(() -> service.submit("9001", allAnsweredReq(true)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场考试已记录为缺考，不能交卷");
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  void submitRejectedAfterDeadlineWithoutAutoSubmit() {
    stubRunningPaper();
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusSeconds(5400));
    when(sessions.selectById(9001L)).thenReturn(session);

    assertThatThrownBy(() -> service.submit("9001", allAnsweredReq(true)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("考试时间已到，请等待系统处理");
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  void saveAfterSubmitIsRejectedAndDoesNotWriteLaterAnswer() {
    stubMapped("submitted", "running");
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusMinutes(5));
    when(sessions.selectById(9001L)).thenReturn(session);
    CompetitionFormalCandidateExamEntity submitted = candidate("submitted");
    submitted.setSubmitTime(SERVER_NOW);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(submitted);

    assertThatThrownBy(() -> service.saveAnswer(
        "9001", "9301",
        new CompetitionFormalExamPlayerAnswerSaveReq(objectMapper.readTree("{\"selectedOptionIds\":[\"9301:A\"]}"),
            false, 8)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场考试已经交卷");
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
    verify(answers, never()).updateById(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void submitSeesNewerVersionIfAnswerSavedFirst() throws Exception {
    stubRunningPaper();
    when(answers.selectByCandidateExamId(9101L)).thenReturn(List.of(
        storedAnswer(9301L, 8, 1), storedAnswer(9302L, 1, 1)));

    assertThatThrownBy(() -> service.submit("9001", versionsReq(true,
        version("9301", 7), version("9302", 1))))
        .extracting("code")
        .isEqualTo(FormalExamErrorCodes.ANSWERS_NOT_SYNCED);
    verify(candidateExams, never()).markSubmittedIfRunning(
        any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  private void stubRunningPaper() {
    stubMapped("running", "running");
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusMinutes(5));
    when(sessions.selectById(9001L)).thenReturn(session);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectByPaperSnapshotId(9201L)).thenReturn(List.of(
        question(9301L, 1), question(9302L, 2)));
  }

  private void stubMapped(String candidateStatus, String sessionStatus) {
    when(sessions.selectById(9001L)).thenReturn(session(sessionStatus));
    when(profiles.selectByPlayerAndFormalSessionScope(1L, 77L, 20L, 10L, 30L, 40L))
        .thenReturn(List.of(profile()));
    when(candidateExams.selectBySessionAndProfileIds(9001L, List.of(8001L)))
        .thenReturn(List.of(candidate(candidateStatus)));
    when(events.getById(1L, 20L)).thenReturn(event());
    when(stages.selectById(10L)).thenReturn(stage());
    when(workTypes.selectById(30L)).thenReturn(workType());
    when(levels.selectById(40L)).thenReturn(level());
  }

  private static CompetitionFormalExamPlayerSubmitReq allAnsweredReq(boolean confirm) {
    return versionsReq(confirm, version("9301", 2), version("9302", 1));
  }

  private static CompetitionFormalExamPlayerSubmitReq versionsReq(
      boolean confirm, CompetitionFormalExamPlayerAnswerVersionItem... items) {
    return new CompetitionFormalExamPlayerSubmitReq(confirm, List.of(items));
  }

  private static CompetitionFormalExamPlayerAnswerVersionItem version(String id, int version) {
    return new CompetitionFormalExamPlayerAnswerVersionItem(id, version);
  }

  private static CompetitionFormalCandidateAnswerEntity storedAnswer(
      Long questionId, int version, int answered) {
    CompetitionFormalCandidateAnswerEntity entity = new CompetitionFormalCandidateAnswerEntity();
    entity.setId(10000L + questionId);
    entity.setExamSessionId(9001L);
    entity.setCandidateExamId(9101L);
    entity.setQuestionSnapshotId(questionId);
    entity.setAnswerJson("{\"selectedOptionIds\":[\"%s:A\"]}".formatted(questionId));
    entity.setIsAnswered(answered);
    entity.setAnswerVersion(version);
    return entity;
  }

  private static CompetitionFormalExamQuestionSnapshotEntity question(Long id, int order) {
    CompetitionFormalExamQuestionSnapshotEntity entity = new CompetitionFormalExamQuestionSnapshotEntity();
    entity.setId(id);
    entity.setTenantId(1L);
    entity.setExamSessionId(9001L);
    entity.setPaperSnapshotId(9201L);
    entity.setQuestionOrder(order);
    entity.setQuestionType("single");
    entity.setStem("题干" + order);
    entity.setScore(BigDecimal.ONE);
    entity.setOptionsJson("[{\"key\":\"A\",\"content\":\"甲\"}]");
    return entity;
  }

  private static CompetitionFormalExamSessionEntity session(String status) {
    CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
    session.setId(9001L);
    session.setTenantId(1L);
    session.setEventId(20L);
    session.setStageId(10L);
    session.setEventWorkTypeId(30L);
    session.setEventWorkTypeLevelId(40L);
    session.setExamType("theory");
    session.setStatus(status);
    session.setDurationSeconds(5400);
    return session;
  }

  private static CompetitionFormalCandidateExamEntity candidate(String status) {
    CompetitionFormalCandidateExamEntity entity = new CompetitionFormalCandidateExamEntity();
    entity.setId(9101L);
    entity.setTenantId(1L);
    entity.setExamSessionId(9001L);
    entity.setContestantProfileId(8001L);
    entity.setPlayerId(77L);
    entity.setCandidateNo("001");
    entity.setCandidateName("张三");
    entity.setPaperSnapshotId(9201L);
    entity.setStatus(status);
    entity.setAnsweredCount(0);
    entity.setTotalQuestionCount(2);
    return entity;
  }

  private static CompetitionContestantProfileEntity profile() {
    CompetitionContestantProfileEntity profile = new CompetitionContestantProfileEntity();
    profile.setId(8001L);
    profile.setTenantId(1L);
    return profile;
  }

  private static CompetitionEventEntity event() {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(20L);
    event.setName("技能大赛");
    return event;
  }

  private static CompetitionStageEntity stage() {
    CompetitionStageEntity stage = new CompetitionStageEntity();
    stage.setId(10L);
    return stage;
  }

  private static CompetitionEventWorkTypeEntity workType() {
    CompetitionEventWorkTypeEntity workType = new CompetitionEventWorkTypeEntity();
    workType.setId(30L);
    workType.setDisplayName("铝电解工");
    return workType;
  }

  private static CompetitionEventWorkTypeLevelEntity level() {
    CompetitionEventWorkTypeLevelEntity level = new CompetitionEventWorkTypeLevelEntity();
    level.setId(40L);
    return level;
  }
}
