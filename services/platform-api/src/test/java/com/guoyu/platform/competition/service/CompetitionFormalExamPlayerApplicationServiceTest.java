package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpLogic;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.dto.CompetitionFormalExamPlayerLoginReq;
import com.guoyu.platform.competition.entity.CompetitionContestantProfileEntity;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeEntity;
import com.guoyu.platform.competition.entity.CompetitionEventWorkTypeLevelEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.entity.CompetitionStageEntity;
import com.guoyu.platform.competition.enums.CompetitionPlayerStatus;
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
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerContextVO;
import com.guoyu.platform.competition.vo.CompetitionFormalExamPlayerLoginVO;
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
import org.springframework.security.crypto.password.PasswordEncoder;

class CompetitionFormalExamPlayerApplicationServiceTest {

  private static final Instant NOW = Instant.parse("2026-09-08T01:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("Asia/Shanghai"));

  private final CompetitionPlayerIdentityMapper playerMapper =
      Mockito.mock(CompetitionPlayerIdentityMapper.class);
  private final CompetitionPlayerIdentityDomainService identityService =
      Mockito.mock(CompetitionPlayerIdentityDomainService.class);
  private final CompetitionPlayerDataHash dataHash = Mockito.mock(CompetitionPlayerDataHash.class);
  private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
  private final StpLogic examStpLogic = Mockito.mock(StpLogic.class);
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
  private final FormalExamCandidateQuestionProjection projection =
      new FormalExamCandidateQuestionProjection(new ObjectMapper());
  private final CompetitionFormalExamAccessSessionService accessSessions =
      Mockito.mock(CompetitionFormalExamAccessSessionService.class);

  private final CompetitionFormalExamPlayerApplicationService service =
      new CompetitionFormalExamPlayerApplicationService(
          playerMapper, identityService, dataHash, passwordEncoder, examStpLogic,
          sessions, candidateExams, profiles, events, stages, workTypes, levels,
          papers, questions, answers, projection, CLOCK, accessSessions);

  @BeforeEach
  void authenticatePlayer() {
    authenticate(77L, 1L);
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void loginReusesIdentityCredentialsAndDoesNotCreateCandidateExam() {
    CompetitionPlayerIdentityEntity player = activePlayer();
    when(identityService.normalizeMobile("13800000000")).thenReturn("13800000000");
    when(dataHash.lookupHash("13800000000")).thenReturn("lookup");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("lookup")).thenReturn(List.of(player));
    when(passwordEncoder.matches("secret12", "encoded")).thenReturn(true);
    when(examStpLogic.isLogin(77L)).thenReturn(false);
    when(examStpLogic.getTokenValue()).thenReturn("formal-token");
    when(examStpLogic.getTokenTimeout()).thenReturn(259200L);
    stubRequestedSessionMembership(9001L, "ready");
    when(accessSessions.issueLogin(any(CompetitionPlayerIdentityEntity.class), eq(9001L), eq("pad-1")))
        .thenReturn(new CompetitionFormalExamAccessSessionService.IssuedAccess(
            55L, 1, "pad-1", 9001L, 9101L));

    CompetitionFormalExamPlayerLoginVO vo = service.login(
        new CompetitionFormalExamPlayerLoginReq("13800000000", "secret12", "9001", "pad-1"));

    assertThat(vo.playerId()).isEqualTo("77");
    assertThat(vo.tenantId()).isEqualTo("1");
    assertThat(vo.accessToken()).isEqualTo("formal-token");
    assertThat(vo.accessSessionId()).isEqualTo("55");
    assertThat(vo.generation()).isEqualTo(1);
    assertThat(vo.deviceId()).isEqualTo("pad-1");
    assertThat(vo.examSessionId()).isEqualTo("9001");
    assertThat(vo.candidateExamId()).isEqualTo("9101");
    verify(candidateExams, never()).insert(any(CompetitionFormalCandidateExamEntity.class));
    verify(candidateExams, never()).selectJoinableByPlayerId(any());
    verify(examStpLogic, never()).isLogin(any());
    verify(accessSessions).attachTokenFingerprint(55L, 77L, "formal-token");
  }

  @Test
  void loginAllowsPendingActivationPlayersWithValidPassword() {
    CompetitionPlayerIdentityEntity player = activePlayer();
    player.setStatus(CompetitionPlayerStatus.PENDING_ACTIVATION.getCode());
    when(identityService.normalizeMobile("13800000000")).thenReturn("13800000000");
    when(dataHash.lookupHash("13800000000")).thenReturn("lookup");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("lookup")).thenReturn(List.of(player));
    when(passwordEncoder.matches("secret12", "encoded")).thenReturn(true);
    when(examStpLogic.isLogin(77L)).thenReturn(false);
    when(examStpLogic.getTokenValue()).thenReturn("formal-token");
    when(examStpLogic.getTokenTimeout()).thenReturn(259200L);
    stubRequestedSessionMembership(9001L, "ready");
    when(accessSessions.issueLogin(any(CompetitionPlayerIdentityEntity.class), eq(9001L), any()))
        .thenReturn(new CompetitionFormalExamAccessSessionService.IssuedAccess(
            55L, 1, "pad-1", 9001L, 9101L));

    CompetitionFormalExamPlayerLoginVO vo = service.login(
        new CompetitionFormalExamPlayerLoginReq("13800000000", "secret12", "9001", "pad-1"));

    assertThat(vo.accessToken()).isEqualTo("formal-token");
    assertThat(vo.tenantId()).isEqualTo("1");
  }

  @Test
  void loginBindsPadWithoutSessionIdToUniqueJoinableExam() {
    CompetitionPlayerIdentityEntity player = activePlayer();
    when(identityService.normalizeMobile("13800000000")).thenReturn("13800000000");
    when(dataHash.lookupHash("13800000000")).thenReturn("lookup");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("lookup")).thenReturn(List.of(player));
    when(passwordEncoder.matches("secret12", "encoded")).thenReturn(true);
    CompetitionFormalCandidateExamEntity joinable = candidate("waiting_enter", null);
    joinable.setExamSessionId(9001L);
    when(candidateExams.selectJoinableByPlayerId(77L)).thenReturn(List.of(joinable));
    when(examStpLogic.getTokenValue()).thenReturn("formal-token");
    when(examStpLogic.getTokenTimeout()).thenReturn(259200L);
    when(accessSessions.issueLogin(any(CompetitionPlayerIdentityEntity.class), eq(9001L), eq("pad-1")))
        .thenReturn(new CompetitionFormalExamAccessSessionService.IssuedAccess(
            55L, 1, "pad-1", 9001L, 9101L));

    CompetitionFormalExamPlayerLoginVO vo = service.login(
        new CompetitionFormalExamPlayerLoginReq("13800000000", "secret12", null, "pad-1"));

    assertThat(vo.examSessionId()).isEqualTo("9001");
    verify(accessSessions).issueLogin(player, 9001L, "pad-1");
    verify(candidateExams, never()).selectBySessionAndPlayerId(any(), any());
  }

  @Test
  void loginRemapsFromPreviousFinishedPadSessionToCurrentJoinableExam() {
    CompetitionPlayerIdentityEntity player = activePlayer();
    when(identityService.normalizeMobile("13800000000")).thenReturn("13800000000");
    when(dataHash.lookupHash("13800000000")).thenReturn("lookup");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("lookup")).thenReturn(List.of(player));
    when(passwordEncoder.matches("secret12", "encoded")).thenReturn(true);
    when(candidateExams.selectBySessionAndPlayerId(8001L, 77L)).thenReturn(List.of());
    when(sessions.selectById(8001L)).thenReturn(sessionAt(8001L, "finished"));
    CompetitionFormalCandidateExamEntity joinable = candidate("waiting_enter", null);
    joinable.setExamSessionId(9001L);
    when(candidateExams.selectJoinableByPlayerId(77L)).thenReturn(List.of(joinable));
    when(examStpLogic.getTokenValue()).thenReturn("formal-token");
    when(examStpLogic.getTokenTimeout()).thenReturn(259200L);
    when(accessSessions.issueLogin(any(CompetitionPlayerIdentityEntity.class), eq(9001L), eq("pad-1")))
        .thenReturn(new CompetitionFormalExamAccessSessionService.IssuedAccess(
            55L, 1, "pad-1", 9001L, 9101L));

    CompetitionFormalExamPlayerLoginVO vo = service.login(
        new CompetitionFormalExamPlayerLoginReq("13800000000", "secret12", "8001", "pad-1"));

    assertThat(vo.examSessionId()).isEqualTo("9001");
    verify(accessSessions).issueLogin(player, 9001L, "pad-1");
    verify(accessSessions, never()).issueLogin(any(), eq(8001L), any());
  }

  @Test
  void loginRejectsWhenRequestedSessionHasNoSeatAndNoJoinableExam() {
    CompetitionPlayerIdentityEntity player = activePlayer();
    when(identityService.normalizeMobile("13800000000")).thenReturn("13800000000");
    when(dataHash.lookupHash("13800000000")).thenReturn("lookup");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("lookup")).thenReturn(List.of(player));
    when(passwordEncoder.matches("secret12", "encoded")).thenReturn(true);
    when(candidateExams.selectBySessionAndPlayerId(8001L, 77L)).thenReturn(List.of());
    when(sessions.selectById(8001L)).thenReturn(sessionAt(8001L, "finished"));
    when(candidateExams.selectJoinableByPlayerId(77L)).thenReturn(List.of());

    assertThatThrownBy(() -> service.login(
        new CompetitionFormalExamPlayerLoginReq("13800000000", "secret12", "8001", "pad-1")))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前账号未参加本场理论考试")
        .extracting("code")
        .isEqualTo("403");
    verify(accessSessions, never()).issueLogin(any(), any(), any());
  }

  @Test
  void loginRequiresQrWhenPlayerHasMultipleJoinableExams() {
    CompetitionPlayerIdentityEntity player = activePlayer();
    when(identityService.normalizeMobile("13800000000")).thenReturn("13800000000");
    when(dataHash.lookupHash("13800000000")).thenReturn("lookup");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("lookup")).thenReturn(List.of(player));
    when(passwordEncoder.matches("secret12", "encoded")).thenReturn(true);
    when(candidateExams.selectBySessionAndPlayerId(8001L, 77L)).thenReturn(List.of());
    CompetitionFormalCandidateExamEntity first = candidate("waiting_enter", null);
    first.setExamSessionId(9001L);
    CompetitionFormalCandidateExamEntity second = candidate("waiting_enter", null);
    second.setId(9102L);
    second.setExamSessionId(9002L);
    when(candidateExams.selectJoinableByPlayerId(77L)).thenReturn(List.of(first, second));

    assertThatThrownBy(() -> service.login(
        new CompetitionFormalExamPlayerLoginReq("13800000000", "secret12", "8001", "pad-1")))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前账号存在多场可参加的理论考试，请扫描本场二维码")
        .extracting("code")
        .isEqualTo("409");
    verify(accessSessions, never()).issueLogin(any(), any(), any());
  }

  @Test
  void loginRejectsDisabledPlayersEvenWithValidPassword() {
    CompetitionPlayerIdentityEntity player = activePlayer();
    player.setStatus(CompetitionPlayerStatus.DISABLED.getCode());
    when(identityService.normalizeMobile("13800000000")).thenReturn("13800000000");
    when(dataHash.lookupHash("13800000000")).thenReturn("lookup");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("lookup")).thenReturn(List.of(player));
    when(passwordEncoder.matches("secret12", "encoded")).thenReturn(true);

    assertThatThrownBy(() -> service.login(
        new CompetitionFormalExamPlayerLoginReq("13800000000", "secret12")))
        .isInstanceOf(BusinessException.class)
        .hasMessage("账号或密码错误")
        .extracting("code")
        .isEqualTo("401");
    verify(examStpLogic, never()).isLogin(any());
  }

  @Test
  void lateRequestFromAnotherSessionIsForbidden() {
    CompetitionExamPrincipal principal =
        new CompetitionExamPrincipal(77L, 1L, 55L, 1, 9999L, 9101L);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));

    assertThatThrownBy(() -> service.context("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前登录不属于本场考试")
        .extracting("code")
        .isEqualTo("403");
    verify(sessions, never()).selectById(any());
  }

  @Test
  void contextMapsIdentityToOwnCandidateExam() throws Exception {
    stubMappedExam("waiting_enter", "ready", null);

    CompetitionFormalExamPlayerContextVO vo = service.context("9001");

    assertThat(vo.examSession().id()).isEqualTo("9001");
    assertThat(vo.examSession().status()).isEqualTo("ready");
    assertThat(vo.candidateExam().id()).isEqualTo("9101");
    assertThat(vo.candidateExam().candidateId()).isEqualTo("8001");
    assertThat(vo.candidateExam().candidateName()).isEqualTo("张三");
    assertThat(vo.competition().eventName()).isEqualTo("技能大赛");
    assertThat(vo.competition().tradeName()).isEqualTo("铝电解工");
    assertThat(vo.serverTime()).isEqualTo(LocalDateTime.ofInstant(NOW, CLOCK.getZone()));
    assertThat(vo.examSession().actualStartTime()).isNull();
    assertThat(vo.examSession().deadlineTime()).isNull();
    assertThat(vo.runtimeIssue()).isNull();
    String json = new ObjectMapper().registerModule(new JavaTimeModule())
        .writeValueAsString(vo);
    assertThat(json)
        .doesNotContain("correctAnswersJson")
        .doesNotContain("fillAnswer")
        .doesNotContain("isCorrect")
        .doesNotContain("awardedScore")
        .doesNotContain("resultStatus")
        .doesNotContain("questionScores")
        .doesNotContain("correctCount")
        .doesNotContain("totalScore");
  }

  @Test
  void contextRejectsPlayerWhoIsNotInThisSession() {
    when(sessions.selectById(9001L)).thenReturn(session("ready"));
    when(profiles.selectByPlayerAndFormalSessionScope(1L, 77L, 20L, 10L, 30L, 40L))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.context("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前账号未参加本场理论考试")
        .extracting("code")
        .isEqualTo("403");
  }

  @Test
  void contextRejectsProfileWithoutCandidateExam() {
    when(sessions.selectById(9001L)).thenReturn(session("ready"));
    when(profiles.selectByPlayerAndFormalSessionScope(1L, 77L, 20L, 10L, 30L, 40L))
        .thenReturn(List.of(profile(8001L)));
    when(candidateExams.selectBySessionAndProfileIds(9001L, List.of(8001L)))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.context("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("未找到当前选手的正式考试实例，请联系工作人员")
        .extracting("code")
        .isEqualTo("422");
    verify(candidateExams, never()).insert(any(CompetitionFormalCandidateExamEntity.class));
  }

  @Test
  void contextRejectsCandidateExamFromAnotherSession() {
    when(sessions.selectById(9002L)).thenReturn(sessionAt(9002L, "ready"));
    when(profiles.selectByPlayerAndFormalSessionScope(1L, 77L, 20L, 10L, 30L, 40L))
        .thenReturn(List.of(profile(8001L)));
    CompetitionFormalCandidateExamEntity otherSession = candidate("waiting_enter", null);
    otherSession.setExamSessionId(9001L);
    when(candidateExams.selectBySessionAndProfileIds(9002L, List.of(8001L)))
        .thenReturn(List.of(otherSession));

    assertThatThrownBy(() -> service.context("9002"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("当前考试不属于本场比赛");
  }

  @Test
  void contextRejectsWhenUnauthenticated() {
    SecurityContextHolder.clearContext();

    assertThatThrownBy(() -> service.context("9001"))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("401");
  }

  @Test
  void enterMovesWaitingEnterToReadyAndWritesEnterTimeOnce() {
    stubMappedExam("waiting_enter", "ready", null);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("waiting_enter", null));
    when(candidateExams.markEnterIfWaiting(
        eq(9101L), eq("waiting_enter"), eq("ready"), any(), any(), eq(77L))).thenReturn(1);
    CompetitionFormalCandidateExamEntity ready = candidate("ready", LocalDateTime.ofInstant(NOW, CLOCK.getZone()));
    when(candidateExams.selectById(9101L)).thenReturn(ready);

    CompetitionFormalExamPlayerContextVO vo = service.enter("9001");

    assertThat(vo.candidateExam().status()).isEqualTo("ready");
    verify(candidateExams).markEnterIfWaiting(
        eq(9101L), eq("waiting_enter"), eq("ready"),
        eq(LocalDateTime.ofInstant(NOW, CLOCK.getZone())),
        eq(LocalDateTime.ofInstant(NOW, CLOCK.getZone())), eq(77L));
  }

  @Test
  void enterIsIdempotentForReadyAndDoesNotRewriteEnterTime() {
    LocalDateTime firstEnter = LocalDateTime.of(2026, 9, 8, 8, 0, 0);
    stubMappedExam("ready", "ready", firstEnter);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("ready", firstEnter));

    CompetitionFormalExamPlayerContextVO vo = service.enter("9001");

    assertThat(vo.candidateExam().status()).isEqualTo("ready");
    verify(candidateExams, never()).markEnterIfWaiting(any(), any(), any(), any(), any(), any());
  }

  @Test
  void enterDoesNotDowngradeRunningCandidate() {
    stubMappedExam("running", "ready", LocalDateTime.of(2026, 9, 8, 8, 0, 0));
    when(candidateExams.selectByIdForUpdate(9101L))
        .thenReturn(candidate("running", LocalDateTime.of(2026, 9, 8, 8, 0, 0)));

    CompetitionFormalExamPlayerContextVO vo = service.enter("9001");

    assertThat(vo.candidateExam().status()).isEqualTo("running");
    verify(candidateExams, never()).markEnterIfWaiting(any(), any(), any(), any(), any(), any());
  }

  @Test
  void enterRejectedWhenSessionPreparing() {
    stubMappedExam("waiting_enter", "preparing", null);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("waiting_enter", null));

    assertThatThrownBy(() -> service.enter("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("正式比赛数据正在准备，请稍候");
    verify(candidateExams, never()).markEnterIfWaiting(any(), any(), any(), any(), any(), any());
  }

  @Test
  void enterRejectedWhenSessionFinished() {
    stubMappedExam("waiting_enter", "finished", null);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("waiting_enter", null));

    assertThatThrownBy(() -> service.enter("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场比赛已经结束");
  }

  @Test
  void enterRejectedWhenSessionCancelled() {
    stubMappedExam("waiting_enter", "cancelled", null);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("waiting_enter", null));

    assertThatThrownBy(() -> service.enter("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场比赛已经取消");
  }

  @Test
  void enterDoesNotAutoAdmitLateCandidateWhenSessionRunning() {
    stubMappedExam("waiting_enter", "running", null);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("waiting_enter", null));

    assertThatThrownBy(() -> service.enter("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("比赛已经开始，请联系现场工作人员");
    verify(candidateExams, never()).markEnterIfWaiting(any(), any(), any(), any(), any(), any());
  }

  @Test
  void contextReturnsRunningTimesWithoutPaperSecrets() {
    stubMappedExam("running", "running", LocalDateTime.of(2026, 9, 8, 8, 0, 0));
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(LocalDateTime.of(2026, 9, 8, 9, 0, 0));
    when(sessions.selectById(9001L)).thenReturn(session);

    CompetitionFormalExamPlayerContextVO vo = service.context("9001");

    assertThat(vo.examSession().status()).isEqualTo("running");
    assertThat(vo.candidateExam().status()).isEqualTo("running");
    assertThat(vo.examSession().actualStartTime()).isEqualTo(LocalDateTime.of(2026, 9, 8, 9, 0, 0));
    assertThat(vo.examSession().deadlineTime()).isEqualTo(LocalDateTime.of(2026, 9, 8, 10, 30, 0));
    assertThat(vo.serverTime()).isEqualTo(LocalDateTime.ofInstant(NOW, CLOCK.getZone()));
    assertThat(vo.runtimeIssue()).isNull();
  }

  @Test
  void contextKeepsWaitingEnterWhenSessionAlreadyRunning() {
    stubMappedExam("waiting_enter", "running", null);

    CompetitionFormalExamPlayerContextVO vo = service.context("9001");

    assertThat(vo.examSession().status()).isEqualTo("running");
    assertThat(vo.candidateExam().status()).isEqualTo("waiting_enter");
    assertThat(vo.runtimeIssue()).isNull();
    verify(candidateExams, never()).markEnterIfWaiting(any(), any(), any(), any(), any(), any());
    verify(candidateExams, never()).markReadyToRunning(any(), any(), any(), any(), any(), any());
  }

  @Test
  void contextExposesAnomalyWhenSessionRunningButCandidateReady() {
    stubMappedExam("ready", "running", LocalDateTime.of(2026, 9, 8, 8, 0, 0));

    CompetitionFormalExamPlayerContextVO vo = service.context("9001");

    assertThat(vo.examSession().status()).isEqualTo("running");
    assertThat(vo.candidateExam().status()).isEqualTo("ready");
    assertThat(vo.runtimeIssue()).contains("运行数据异常");
    verify(candidateExams, never()).markReadyToRunning(any(), any(), any(), any(), any(), any());
  }

  @Test
  void contextReturnsSubmitTimeWhenCandidateSubmitted() {
    stubMappedExam("submitted", "running", LocalDateTime.of(2026, 9, 8, 8, 0, 0));
    CompetitionFormalCandidateExamEntity submitted = candidate("submitted", LocalDateTime.of(2026, 9, 8, 8, 0, 0));
    submitted.setSubmitTime(LocalDateTime.of(2026, 9, 8, 9, 26, 18));
    submitted.setSubmitType(FormalExamErrorCodes.SUBMIT_TYPE_MANUAL);
    when(candidateExams.selectBySessionAndProfileIds(9001L, List.of(8001L)))
        .thenReturn(List.of(submitted));
    CompetitionFormalExamSessionEntity session = session("running");
    session.setActualStartTime(LocalDateTime.of(2026, 9, 8, 9, 0, 0));
    when(sessions.selectById(9001L)).thenReturn(session);

    CompetitionFormalExamPlayerContextVO vo = service.context("9001");

    assertThat(vo.candidateExam().status()).isEqualTo("submitted");
    assertThat(vo.candidateExam().submitTime()).isEqualTo(LocalDateTime.of(2026, 9, 8, 9, 26, 18));
    assertThat(vo.candidateExam().candidateName()).isEqualTo("张三");
    assertThat(vo.candidateExam().candidateNo()).isEqualTo("001");
  }

  @Test
  void enterRejectedForSubmittedCandidate() {
    stubMappedExam("submitted", "ready", LocalDateTime.of(2026, 9, 8, 8, 0, 0));
    when(candidateExams.selectByIdForUpdate(9101L))
        .thenReturn(candidate("submitted", LocalDateTime.of(2026, 9, 8, 8, 0, 0)));

    assertThatThrownBy(() -> service.enter("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场考试已经交卷，不能重新进入候考");
  }

  @Test
  void enterRejectedForAbsentCandidate() {
    stubMappedExam("absent", "finished", null);
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate("absent", null));

    assertThatThrownBy(() -> service.enter("9001"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("本场考试已记录为缺考，不能进入候考");
    verify(candidateExams, never()).markEnterIfWaiting(any(), any(), any(), any(), any(), any());
  }

  @Test
  void contextKeepsPersonalTerminalWhenSessionFinished() {
    stubMappedExam("submitted", "finished", LocalDateTime.of(2026, 9, 8, 8, 0, 0));
    CompetitionFormalCandidateExamEntity submitted =
        candidate("submitted", LocalDateTime.of(2026, 9, 8, 8, 0, 0));
    submitted.setSubmitTime(LocalDateTime.of(2026, 9, 8, 9, 26, 18));
    when(candidateExams.selectBySessionAndProfileIds(9001L, List.of(8001L)))
        .thenReturn(List.of(submitted));
    CompetitionFormalExamSessionEntity session = session("finished");
    session.setActualStartTime(LocalDateTime.of(2026, 9, 8, 9, 0, 0));
    when(sessions.selectById(9001L)).thenReturn(session);

    CompetitionFormalExamPlayerContextVO vo = service.context("9001");

    assertThat(vo.examSession().status()).isEqualTo("finished");
    assertThat(vo.candidateExam().status()).isEqualTo("submitted");
  }

  @Test
  void contextReturnsAbsentStatus() {
    stubMappedExam("absent", "finished", null);

    CompetitionFormalExamPlayerContextVO vo = service.context("9001");

    assertThat(vo.examSession().status()).isEqualTo("finished");
    assertThat(vo.candidateExam().status()).isEqualTo("absent");
    assertThat(vo.runtimeIssue()).isNull();
  }

  private void stubRequestedSessionMembership(Long sessionId, String sessionStatus) {
    when(candidateExams.selectBySessionAndPlayerId(sessionId, 77L))
        .thenReturn(List.of(candidate("waiting_enter", null)));
    when(sessions.selectById(sessionId)).thenReturn(sessionAt(sessionId, sessionStatus));
  }

  private void stubMappedExam(String candidateStatus, String sessionStatus, LocalDateTime enterTime) {
    when(sessions.selectById(9001L)).thenReturn(session(sessionStatus));
    when(profiles.selectByPlayerAndFormalSessionScope(1L, 77L, 20L, 10L, 30L, 40L))
        .thenReturn(List.of(profile(8001L)));
    when(candidateExams.selectBySessionAndProfileIds(9001L, List.of(8001L)))
        .thenReturn(List.of(candidate(candidateStatus, enterTime)));
    when(events.getById(1L, 20L)).thenReturn(event());
    when(stages.selectById(10L)).thenReturn(stage());
    when(workTypes.selectById(30L)).thenReturn(workType());
    when(levels.selectById(40L)).thenReturn(level());
  }

  private static void authenticate(Long playerId, Long tenantId) {
    CompetitionExamPrincipal principal = new CompetitionExamPrincipal(playerId, tenantId);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities()));
  }

  private static CompetitionPlayerIdentityEntity activePlayer() {
    CompetitionPlayerIdentityEntity player = new CompetitionPlayerIdentityEntity();
    player.setId(77L);
    player.setTenantId(1L);
    player.setStatus(CompetitionPlayerStatus.ACTIVE.getCode());
    player.setPasswordHash("encoded");
    return player;
  }

  private static CompetitionFormalExamSessionEntity session(String status) {
    return sessionAt(9001L, status);
  }

  private static CompetitionFormalExamSessionEntity sessionAt(Long id, String status) {
    CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
    session.setId(id);
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

  private static CompetitionContestantProfileEntity profile(Long id) {
    CompetitionContestantProfileEntity profile = new CompetitionContestantProfileEntity();
    profile.setId(id);
    profile.setTenantId(1L);
    profile.setEventWorkTypeId(30L);
    profile.setEventWorkTypeLevelId(40L);
    return profile;
  }

  private static CompetitionFormalCandidateExamEntity candidate(String status, LocalDateTime enterTime) {
    CompetitionFormalCandidateExamEntity entity = new CompetitionFormalCandidateExamEntity();
    entity.setId(9101L);
    entity.setExamSessionId(9001L);
    entity.setContestantProfileId(8001L);
    entity.setPlayerId(77L);
    entity.setCandidateNo("001");
    entity.setCandidateName("张三");
    entity.setPaperSnapshotId(9201L);
    entity.setStatus(status);
    entity.setEnterTime(enterTime);
    return entity;
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
    stage.setName("初赛");
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
    level.setLevelName("高级工");
    return level;
  }
}
