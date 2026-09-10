package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpLogic;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerAnswerSaveReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerAnswerSyncItemReq;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerAnswerSyncReq;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateAnswerEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamPaperSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamQuestionSnapshotEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.exam.formal.FormalExamCandidateQuestionProjection;
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
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerAnswerSaveVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerPaperVO;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

class CompetitionFormalExamPlayerPaperServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-08T01:00:00Z");
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
    CompetitionExamPrincipal principal = new CompetitionExamPrincipal(77L, 1L);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void paperReturnsOwnQuestionsInOrderWithoutSecrets() throws Exception {
    stubRunning();
    when(papers.selectById(9201L)).thenReturn(paper());
    when(questions.selectByPaperSnapshotId(9201L)).thenReturn(List.of(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"},{\"key\":\"B\",\"content\":\"乙\"}]",
            "[\"A\"]", null),
        question(9302L, 2, "fill", "[]", null, "标准填空")));
    when(answers.selectByCandidateExamId(9101L)).thenReturn(List.of());

    CompetitionFormalExamPlayerPaperVO vo = service.paper("9001");
    String json = objectMapper.writeValueAsString(vo);

    assertThat(vo.questions()).hasSize(2);
    assertThat(vo.questions().get(0).questionSnapshotId()).isEqualTo("9301");
    assertThat(vo.questions().get(0).order()).isEqualTo(1);
    assertThat(vo.questions().get(0).type()).isEqualTo("single");
    assertThat(vo.questions().get(0).options()).extracting(item -> item.optionId())
        .containsExactly("9301:A", "9301:B");
    assertThat(vo.paper().questionCount()).isEqualTo(2);
    assertThat(json)
        .doesNotContain("correctAnswersJson")
        .doesNotContain("fillAnswer")
        .doesNotContain("sourceQuestionId")
        .doesNotContain("sourcePaperId")
        .doesNotContain("paperSnapshotId")
        .doesNotContain("paperCode")
        .doesNotContain("标准填空")
        .doesNotContain("[\"A\"]")
        .doesNotContain("isCorrect")
        .doesNotContain("awardedScore")
        .doesNotContain("resultStatus")
        .doesNotContain("questionScores");
  }

  @Test
  void paperRestoresSavedAnswers() {
    stubRunning();
    when(papers.selectById(9201L)).thenReturn(paper());
    when(questions.selectByPaperSnapshotId(9201L)).thenReturn(List.of(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"}]", "[\"A\"]", null)));
    CompetitionFormalCandidateAnswerEntity saved = storedAnswer(9301L, 2, "{\"selectedOptionIds\":[\"9301:A\"]}", 1);
    when(answers.selectByCandidateExamId(9101L)).thenReturn(List.of(saved));

    CompetitionFormalExamPlayerPaperVO vo = service.paper("9001");

    assertThat(vo.answers()).hasSize(1);
    assertThat(vo.answers().getFirst().questionSnapshotId()).isEqualTo("9301");
    assertThat(vo.answers().getFirst().version()).isEqualTo(2);
    assertThat(vo.answers().getFirst().isAnswered()).isTrue();
    assertThat(vo.answers().getFirst().answer().get("selectedOptionIds").get(0).asText())
        .isEqualTo("9301:A");
  }

  @Test
  void paperRejectsReadyCandidate() {
    stubMapped("ready", "ready");
    assertThatThrownBy(() -> service.paper("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("比赛尚未开始");
    verify(questions, never()).selectByPaperSnapshotId(any());
  }

  @Test
  void paperRejectsWaitingEnterWhenSessionRunning() {
    stubMapped("waiting_enter", "running");
    assertThatThrownBy(() -> service.paper("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("比赛已经开始，请联系现场工作人员");
  }

  @Test
  void paperRejectsFinishedSession() {
    stubMapped("running", "finished");
    assertThatThrownBy(() -> service.paper("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场比赛已经结束");
  }

  @Test
  void saveCreatesFirstAnswerAndIncrementsAnsweredCount() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"}]", "[\"A\"]", null));
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(null);
    when(answers.insert(any(CompetitionFormalCandidateAnswerEntity.class))).thenReturn(1);
    when(answers.countAnswered(9101L)).thenReturn(1);
    when(candidateExams.updateAnsweredCount(eq(9101L), eq(1), eq(SERVER_NOW), eq(77L))).thenReturn(1);

    CompetitionFormalExamPlayerAnswerSaveVO vo = service.saveAnswer(
        "9001", "9301", req(choice("9301:A"), false, 1));

    ArgumentCaptor<CompetitionFormalCandidateAnswerEntity> captor =
        ArgumentCaptor.forClass(CompetitionFormalCandidateAnswerEntity.class);
    verify(answers).insert(captor.capture());
    assertThat(vo.accepted()).isTrue();
    assertThat(vo.version()).isEqualTo(1);
    assertThat(vo.isAnswered()).isTrue();
    assertThat(vo.answeredCount()).isEqualTo(1);
    assertThat(captor.getValue().getQuestionSnapshotId()).isEqualTo(9301L);
    assertThat(captor.getValue().getCandidateExamId()).isEqualTo(9101L);
    assertThat(captor.getValue().getAnswerVersion()).isEqualTo(1);
    assertThat(captor.getValue().getFirstAnswerTime()).isEqualTo(SERVER_NOW);
  }

  @Test
  void saveAcceptsNextVersion() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"},{\"key\":\"B\",\"content\":\"乙\"}]",
            "[\"A\"]", null));
    CompetitionFormalCandidateAnswerEntity current =
        storedAnswer(9301L, 1, "{\"selectedOptionIds\":[\"9301:A\"]}", 1);
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(current);
    when(answers.updateById(current)).thenReturn(1);
    when(answers.countAnswered(9101L)).thenReturn(1);
    when(candidateExams.updateAnsweredCount(eq(9101L), eq(1), eq(SERVER_NOW), eq(77L))).thenReturn(1);

    CompetitionFormalExamPlayerAnswerSaveVO vo = service.saveAnswer(
        "9001", "9301", req(choice("9301:B"), false, 2));

    assertThat(vo.accepted()).isTrue();
    assertThat(vo.version()).isEqualTo(2);
    assertThat(current.getAnswerVersion()).isEqualTo(2);
    assertThat(current.getFirstAnswerTime()).isEqualTo(LocalDateTime.of(2026, 9, 8, 8, 0));
    verify(answers).updateById(current);
  }

  @Test
  void sameVersionSamePayloadIsIdempotent() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"}]", "[\"A\"]", null));
    CompetitionFormalCandidateAnswerEntity current =
        storedAnswer(9301L, 2, "{\"selectedOptionIds\":[\"9301:A\"]}", 1);
    LocalDateTime first = current.getFirstAnswerTime();
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(current);

    CompetitionFormalExamPlayerAnswerSaveVO vo = service.saveAnswer(
        "9001", "9301", req(choice("9301:A"), false, 2));

    assertThat(vo.accepted()).isTrue();
    assertThat(vo.version()).isEqualTo(2);
    assertThat(current.getFirstAnswerTime()).isEqualTo(first);
    verify(answers, never()).updateById(any(CompetitionFormalCandidateAnswerEntity.class));
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void sameVersionDifferentPayloadConflicts() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"},{\"key\":\"B\",\"content\":\"乙\"}]",
            "[\"A\"]", null));
    CompetitionFormalCandidateAnswerEntity current =
        storedAnswer(9301L, 2, "{\"selectedOptionIds\":[\"9301:A\"]}", 1);
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(current);

    CompetitionFormalExamPlayerAnswerSaveVO vo = service.saveAnswer(
        "9001", "9301", req(choice("9301:B"), false, 2));

    assertThat(vo.accepted()).isFalse();
    assertThat(vo.conflictCode()).isEqualTo("VERSION_PAYLOAD_CONFLICT");
    assertThat(vo.version()).isEqualTo(2);
    verify(answers, never()).updateById(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void olderVersionDoesNotOverwrite() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"},{\"key\":\"B\",\"content\":\"乙\"}]",
            "[\"A\"]", null));
    CompetitionFormalCandidateAnswerEntity current =
        storedAnswer(9301L, 2, "{\"selectedOptionIds\":[\"9301:B\"]}", 1);
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(current);

    CompetitionFormalExamPlayerAnswerSaveVO vo = service.saveAnswer(
        "9001", "9301", req(choice("9301:A"), false, 1));

    assertThat(vo.accepted()).isFalse();
    assertThat(vo.conflictCode()).isEqualTo("STALE_VERSION");
    assertThat(current.getAnswerVersion()).isEqualTo(2);
    verify(answers, never()).updateById(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void clearingAnswerDecrementsAnsweredCount() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"}]", "[\"A\"]", null));
    CompetitionFormalCandidateAnswerEntity current =
        storedAnswer(9301L, 1, "{\"selectedOptionIds\":[\"9301:A\"]}", 1);
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(current);
    when(answers.updateById(current)).thenReturn(1);
    when(answers.countAnswered(9101L)).thenReturn(0);
    when(candidateExams.updateAnsweredCount(eq(9101L), eq(0), eq(SERVER_NOW), eq(77L))).thenReturn(1);

    ObjectNode empty = objectMapper.createObjectNode();
    empty.putArray("selectedOptionIds");
    CompetitionFormalExamPlayerAnswerSaveVO vo = service.saveAnswer(
        "9001", "9301", req(empty, false, 2));

    assertThat(vo.accepted()).isTrue();
    assertThat(vo.isAnswered()).isFalse();
    assertThat(vo.answeredCount()).isZero();
  }

  @Test
  void markOnlyDoesNotChangeAnsweredCount() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"}]", "[\"A\"]", null));
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(null);
    when(answers.insert(any(CompetitionFormalCandidateAnswerEntity.class))).thenReturn(1);
    when(answers.countAnswered(9101L)).thenReturn(0);
    when(candidateExams.updateAnsweredCount(eq(9101L), eq(0), eq(SERVER_NOW), eq(77L))).thenReturn(1);

    ObjectNode empty = objectMapper.createObjectNode();
    empty.putArray("selectedOptionIds");
    CompetitionFormalExamPlayerAnswerSaveVO vo = service.saveAnswer(
        "9001", "9301", req(empty, true, 1));

    assertThat(vo.accepted()).isTrue();
    assertThat(vo.marked()).isTrue();
    assertThat(vo.isAnswered()).isFalse();
    assertThat(vo.answeredCount()).isZero();
  }

  @Test
  void saveRejectsQuestionFromAnotherPaper() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    CompetitionFormalExamQuestionSnapshotEntity other =
        question(9401L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"}]", "[\"A\"]", null);
    other.setPaperSnapshotId(9999L);
    when(questions.selectById(9401L)).thenReturn(other);

    assertThatThrownBy(() -> service.saveAnswer("9001", "9401", req(choice("9401:A"), false, 1)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("题目不属于当前试卷");
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void saveRejectsAfterDeadline() {
    stubRunning();
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusSeconds(5400));
    when(sessions.selectById(9001L)).thenReturn(session);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));

    assertThatThrownBy(() -> service.saveAnswer("9001", "9301", req(choice("9301:A"), false, 1)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("考试时间已到，请等待系统处理")
        .extracting("code")
        .isEqualTo("409");
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void saveRejectsReadySession() {
    stubMapped("ready", "ready");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("ready"));
    assertThatThrownBy(() -> service.saveAnswer("9001", "9301", req(choice("9301:A"), false, 1)))
        .hasMessage("比赛尚未开始");
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void paperRejectsSubmittedCandidate() {
    stubMapped("submitted", "running");
    assertThatThrownBy(() -> service.paper("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场考试已经交卷");
    verify(questions, never()).selectByPaperSnapshotId(any());
  }

  @Test
  void paperRejectsAutoSubmittedCandidate() {
    stubMapped("auto_submitted", "running");
    assertThatThrownBy(() -> service.paper("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场考试已经交卷");
    verify(questions, never()).selectByPaperSnapshotId(any());
  }

  @Test
  void paperRejectsForcedSubmittedCandidate() {
    stubMapped("forced_submitted", "running");
    assertThatThrownBy(() -> service.paper("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场考试已经交卷");
    verify(questions, never()).selectByPaperSnapshotId(any());
  }

  @Test
  void paperRejectsAbsentCandidate() {
    stubMapped("absent", "finished");
    assertThatThrownBy(() -> service.paper("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场考试已记录为缺考");
    verify(questions, never()).selectByPaperSnapshotId(any());
  }

  @Test
  void saveRejectsSubmittedCandidate() {
    stubMapped("submitted", "running");
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusMinutes(5));
    when(sessions.selectById(9001L)).thenReturn(session);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("submitted"));

    assertThatThrownBy(() -> service.saveAnswer("9001", "9301", req(choice("9301:A"), false, 1)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场考试已经交卷");
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
    verify(answers, never()).updateById(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void saveRejectsAutoSubmittedCandidate() {
    stubMapped("auto_submitted", "running");
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusMinutes(5));
    when(sessions.selectById(9001L)).thenReturn(session);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("auto_submitted"));

    assertThatThrownBy(() -> service.saveAnswer("9001", "9301", req(choice("9301:A"), false, 1)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场考试已经交卷");
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void saveRejectsForcedSubmittedCandidate() {
    stubMapped("forced_submitted", "running");
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusMinutes(5));
    when(sessions.selectById(9001L)).thenReturn(session);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("forced_submitted"));

    assertThatThrownBy(() -> service.saveAnswer("9001", "9301", req(choice("9301:A"), false, 1)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场考试已经交卷");
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void saveRejectsAbsentCandidate() {
    stubMapped("absent", "finished");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("absent"));

    assertThatThrownBy(() -> service.saveAnswer("9001", "9301", req(choice("9301:A"), false, 1)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场考试已记录为缺考");
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void syncRejectsSubmittedCandidateWithoutWriting() {
    stubMapped("submitted", "running");
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusMinutes(5));
    when(sessions.selectById(9001L)).thenReturn(session);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("submitted"));

    var vo = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 8))));

    assertThat(vo.results()).hasSize(1);
    assertThat(vo.results().getFirst().accepted()).isFalse();
    assertThat(vo.results().getFirst().status()).isEqualTo("CONFLICT");
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
    verify(answers, never()).updateById(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void syncRejectsAutoSubmittedCandidateWithoutWriting() {
    stubMapped("auto_submitted", "running");
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusMinutes(5));
    when(sessions.selectById(9001L)).thenReturn(session);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("auto_submitted"));

    var vo = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 8))));

    assertThat(vo.results()).hasSize(1);
    assertThat(vo.results().getFirst().accepted()).isFalse();
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void syncRejectsForcedSubmittedCandidateWithoutWriting() {
    stubMapped("forced_submitted", "running");
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusMinutes(5));
    when(sessions.selectById(9001L)).thenReturn(session);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("forced_submitted"));

    var vo = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 8))));

    assertThat(vo.results()).hasSize(1);
    assertThat(vo.results().getFirst().accepted()).isFalse();
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void syncRejectsAbsentCandidateWithoutWriting() {
    stubMapped("absent", "finished");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("absent"));

    var vo = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 8))));

    assertThat(vo.results()).hasSize(1);
    assertThat(vo.results().getFirst().accepted()).isFalse();
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void syncAcceptsMultipleQuestionsWithoutRollingBackSiblings() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"}]", "[\"A\"]", null));
    when(questions.selectById(9302L)).thenReturn(
        question(9302L, 2, "single", "[{\"key\":\"A\",\"content\":\"丙\"}]", "[\"A\"]", null));
    CompetitionFormalExamQuestionSnapshotEntity other =
        question(9401L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"}]", "[\"A\"]", null);
    other.setPaperSnapshotId(9999L);
    when(questions.selectById(9401L)).thenReturn(other);
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(null);
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9302L)).thenReturn(null);
    when(answers.insert(any(CompetitionFormalCandidateAnswerEntity.class))).thenReturn(1);
    when(answers.countAnswered(9101L)).thenReturn(1, 2);
    when(candidateExams.updateAnsweredCount(eq(9101L), any(Integer.class), eq(SERVER_NOW), eq(77L)))
        .thenReturn(1);

    var vo = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 1),
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9401", choice("9401:A"), false, 1),
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9302", choice("9302:A"), false, 1))));

    assertThat(vo.results()).hasSize(3);
    assertThat(vo.results().get(0).accepted()).isTrue();
    assertThat(vo.results().get(0).status()).isEqualTo("SYNCED");
    assertThat(vo.results().get(1).accepted()).isFalse();
    assertThat(vo.results().get(1).status()).isEqualTo("FORBIDDEN");
    assertThat(vo.results().get(2).accepted()).isTrue();
    assertThat(vo.results().get(2).status()).isEqualTo("SYNCED");
  }

  @Test
  void syncReturnsStaleAndConflictPerQuestion() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"},{\"key\":\"B\",\"content\":\"乙\"}]",
            "[\"A\"]", null));
    CompetitionFormalCandidateAnswerEntity current =
        storedAnswer(9301L, 5, "{\"selectedOptionIds\":[\"9301:A\"]}", 1);
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(current);

    var stale = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:B"), false, 3))));
    assertThat(stale.results().getFirst().status()).isEqualTo("STALE_VERSION");
    assertThat(stale.results().getFirst().accepted()).isFalse();

    var conflict = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:B"), false, 5))));
    assertThat(conflict.results().getFirst().status()).isEqualTo("VERSION_PAYLOAD_CONFLICT");
  }

  @Test
  void syncRejectsExpiredDeadlinePerItemWithoutThrowing() {
    stubRunning();
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusSeconds(5400));
    when(sessions.selectById(9001L)).thenReturn(session);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));

    var vo = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 1))));

    assertThat(vo.results()).hasSize(1);
    assertThat(vo.results().getFirst().accepted()).isFalse();
    assertThat(vo.results().getFirst().status()).isEqualTo("EXAM_TIME_EXPIRED");
    verify(answers, never()).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void syncRejectsReadySessionPerItem() {
    stubMapped("ready", "ready");
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("ready"));
    var vo = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 1))));
    assertThat(vo.results().getFirst().accepted()).isFalse();
    assertThat(vo.results().getFirst().status()).isEqualTo("CONFLICT");
  }

  @Test
  void syncRejectsOversizedBatch() {
    List<CompetitionFormalExamPlayerAnswerSyncItemReq> items = new java.util.ArrayList<>();
    for (int i = 0; i < 101; i++) {
      items.add(new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 1));
    }
    assertThatThrownBy(() -> service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(items)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("100");
  }

  @Test
  void syncAcceptsAllQuestionsInOneBatch() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"}]", "[\"A\"]", null));
    when(questions.selectById(9302L)).thenReturn(
        question(9302L, 2, "single", "[{\"key\":\"A\",\"content\":\"丙\"}]", "[\"A\"]", null));
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(null);
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9302L)).thenReturn(null);
    when(answers.insert(any(CompetitionFormalCandidateAnswerEntity.class))).thenReturn(1);
    when(answers.countAnswered(9101L)).thenReturn(1, 2);
    when(candidateExams.updateAnsweredCount(eq(9101L), any(Integer.class), eq(SERVER_NOW), eq(77L)))
        .thenReturn(1);

    var vo = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 1),
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9302", choice("9302:A"), true, 1))));

    assertThat(vo.results()).hasSize(2);
    assertThat(vo.results()).allMatch(CompetitionFormalExamPlayerAnswerSaveVO::accepted);
    assertThat(vo.results()).extracting(CompetitionFormalExamPlayerAnswerSaveVO::status)
        .containsExactly("SYNCED", "SYNCED");
  }

  @Test
  void syncTwentyNewAnswersResolvesScopeOnceAndRecountsOnce() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    List<CompetitionFormalExamPlayerAnswerSyncItemReq> items = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      long questionId = 9301L + i;
      when(questions.selectById(questionId)).thenReturn(
          question(questionId, i + 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"}]", "[\"A\"]", null));
      when(answers.selectByCandidateQuestionForUpdate(9101L, questionId)).thenReturn(null);
      items.add(new CompetitionFormalExamPlayerAnswerSyncItemReq(
          String.valueOf(questionId), choice(questionId + ":A"), false, 1));
    }
    when(answers.insert(any(CompetitionFormalCandidateAnswerEntity.class))).thenReturn(1);
    when(answers.countAnswered(9101L)).thenReturn(20);
    when(candidateExams.updateAnsweredCount(eq(9101L), eq(20), eq(SERVER_NOW), eq(77L))).thenReturn(1);

    var vo = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(items));

    assertThat(vo.results()).hasSize(20);
    assertThat(vo.results()).allMatch(CompetitionFormalExamPlayerAnswerSaveVO::accepted);
    assertThat(vo.results()).extracting(CompetitionFormalExamPlayerAnswerSaveVO::answeredCount)
        .containsOnly(20);
    verify(sessions, times(1)).selectById(9001L);
    verify(candidateExams, times(1)).selectByIdForUpdate(9101L);
    verify(questions, times(1)).selectByPaperSnapshotIdAndIds(eq(9201L), any());
    verify(answers, times(1)).selectByCandidateQuestionIdsForUpdate(eq(9101L), any());
    verify(answers, times(1)).countAnswered(9101L);
    verify(candidateExams, times(1)).updateAnsweredCount(eq(9101L), eq(20), eq(SERVER_NOW), eq(77L));
    verify(answers, times(20)).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void syncRejectsDuplicateQuestionSnapshotIdsAfterFirst() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"}]", "[\"A\"]", null));
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(null);
    when(answers.insert(any(CompetitionFormalCandidateAnswerEntity.class))).thenReturn(1);
    when(answers.countAnswered(9101L)).thenReturn(1);
    when(candidateExams.updateAnsweredCount(eq(9101L), any(Integer.class), eq(SERVER_NOW), eq(77L)))
        .thenReturn(1);

    var vo = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 1),
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 1))));

    assertThat(vo.results()).hasSize(2);
    assertThat(vo.results().get(0).accepted()).isTrue();
    assertThat(vo.results().get(1).accepted()).isFalse();
    assertThat(vo.results().get(1).status()).isEqualTo("REJECTED");
    verify(answers, times(1)).insert(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void syncSequentialBatchesUpdateTheSameQuestionByVersion() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"},{\"key\":\"B\",\"content\":\"乙\"}]",
            "[\"A\"]", null));
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(null);
    when(answers.insert(any(CompetitionFormalCandidateAnswerEntity.class))).thenReturn(1);
    when(answers.countAnswered(9101L)).thenReturn(1);
    when(candidateExams.updateAnsweredCount(eq(9101L), any(Integer.class), eq(SERVER_NOW), eq(77L)))
        .thenReturn(1);

    var first = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 1))));
    assertThat(first.results().getFirst().accepted()).isTrue();

    CompetitionFormalCandidateAnswerEntity stored =
        storedAnswer(9301L, 1, "{\"selectedOptionIds\":[\"9301:A\"]}", 1);
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(stored);
    when(answers.updateById(any(CompetitionFormalCandidateAnswerEntity.class))).thenReturn(1);

    var second = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:B"), false, 2))));
    assertThat(second.results().getFirst().accepted()).isTrue();
    assertThat(second.results().getFirst().version()).isEqualTo(2);
    verify(answers, times(1)).insert(any(CompetitionFormalCandidateAnswerEntity.class));
    verify(answers, times(1)).updateById(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  @Test
  void statusReadsCandidateByPlayerWithoutEventGraph() {
    stubRunning();
    when(candidateExams.selectBySessionAndPlayerId(9001L, 77L))
        .thenReturn(List.of(candidate("running")));

    var vo = service.status("9001");

    assertThat(vo.candidateExam().status()).isEqualTo("running");
    assertThat(vo.examSession().id()).isEqualTo("9001");
    verify(events, never()).getById(any(), any());
    verify(profiles, never()).selectByPlayerAndFormalSessionScope(
        any(), any(), any(), any(), any(), any());
  }

  @Test
  void syncRepeatBatchIsIdempotentForSameVersionAndPayload() {
    stubRunning();
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("running"));
    when(questions.selectById(9301L)).thenReturn(
        question(9301L, 1, "single", "[{\"key\":\"A\",\"content\":\"甲\"}]", "[\"A\"]", null));
    CompetitionFormalCandidateAnswerEntity current =
        storedAnswer(9301L, 2, "{\"selectedOptionIds\":[\"9301:A\"]}", 1);
    when(answers.selectByCandidateQuestionForUpdate(9101L, 9301L)).thenReturn(current);

    var vo = service.syncAnswers("9001", new CompetitionFormalExamPlayerAnswerSyncReq(List.of(
        new CompetitionFormalExamPlayerAnswerSyncItemReq("9301", choice("9301:A"), false, 2))));

    assertThat(vo.results().getFirst().accepted()).isTrue();
    assertThat(vo.results().getFirst().status()).isEqualTo("SYNCED");
    assertThat(vo.results().getFirst().version()).isEqualTo(2);
    verify(answers, never()).updateById(any(CompetitionFormalCandidateAnswerEntity.class));
  }

  private void stubRunning() {
    stubMapped("running", "running");
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(SERVER_NOW.minusMinutes(5));
    when(sessions.selectById(9001L)).thenReturn(session);
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
    when(questions.selectByPaperSnapshotIdAndIds(eq(9201L), any())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      List<Long> ids = invocation.getArgument(1);
      List<CompetitionFormalExamQuestionSnapshotEntity> found = new ArrayList<>();
      if (ids == null) {
        return found;
      }
      for (Long id : ids) {
        CompetitionFormalExamQuestionSnapshotEntity question = questions.selectById(id);
        if (question != null && java.util.Objects.equals(question.getPaperSnapshotId(), 9201L)) {
          found.add(question);
        }
      }
      return found;
    });
    when(answers.selectByCandidateQuestionIdsForUpdate(eq(9101L), any())).thenAnswer(invocation -> {
      @SuppressWarnings("unchecked")
      List<Long> ids = invocation.getArgument(1);
      List<CompetitionFormalCandidateAnswerEntity> found = new ArrayList<>();
      if (ids == null) {
        return found;
      }
      for (Long id : ids) {
        CompetitionFormalCandidateAnswerEntity answer =
            answers.selectByCandidateQuestionForUpdate(9101L, id);
        if (answer != null) {
          found.add(answer);
        }
      }
      return found;
    });
  }

  private CompetitionFormalExamPlayerAnswerSaveReq req(ObjectNode answer, boolean marked, int version) {
    return new CompetitionFormalExamPlayerAnswerSaveReq(answer, marked, version);
  }

  private ObjectNode choice(String optionId) {
    ObjectNode node = objectMapper.createObjectNode();
    node.putArray("selectedOptionIds").add(optionId);
    return node;
  }

  private static CompetitionFormalCandidateAnswerEntity storedAnswer(
      Long questionId, int version, String json, int answered) {
    CompetitionFormalCandidateAnswerEntity entity = new CompetitionFormalCandidateAnswerEntity();
    entity.setId(10001L);
    entity.setExamSessionId(9001L);
    entity.setCandidateExamId(9101L);
    entity.setQuestionSnapshotId(questionId);
    entity.setAnswerJson(json);
    entity.setIsAnswered(answered);
    entity.setIsMarked(0);
    entity.setAnswerVersion(version);
    entity.setFirstAnswerTime(LocalDateTime.of(2026, 9, 8, 8, 0));
    entity.setLastAnswerTime(LocalDateTime.of(2026, 9, 8, 8, 0));
    return entity;
  }

  private static CompetitionFormalExamQuestionSnapshotEntity question(
      Long id, int order, String type, String options, String correct, String fill) {
    CompetitionFormalExamQuestionSnapshotEntity entity = new CompetitionFormalExamQuestionSnapshotEntity();
    entity.setId(id);
    entity.setTenantId(1L);
    entity.setExamSessionId(9001L);
    entity.setPaperSnapshotId(9201L);
    entity.setSourceQuestionId(555L);
    entity.setQuestionOrder(order);
    entity.setQuestionType(type);
    entity.setStem("题干" + order);
    entity.setScore(BigDecimal.ONE);
    entity.setOptionsJson(options);
    entity.setCorrectAnswersJson(correct);
    entity.setFillAnswer(fill);
    return entity;
  }

  private static CompetitionFormalExamPaperSnapshotEntity paper() {
    CompetitionFormalExamPaperSnapshotEntity paper = new CompetitionFormalExamPaperSnapshotEntity();
    paper.setId(9201L);
    paper.setExamSessionId(9001L);
    paper.setSourcePaperId(500L);
    paper.setPaperCode("A");
    paper.setTotalScore(new BigDecimal("100"));
    paper.setQuestionCount(2);
    return paper;
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
