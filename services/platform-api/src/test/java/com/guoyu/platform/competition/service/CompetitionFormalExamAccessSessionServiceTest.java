package com.guoyu.platform.competition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpLogic;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.competition.entity.CompetitionFormalCandidateExamEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessCommandEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamAccessSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionFormalExamSessionEntity;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.exam.formal.CompetitionFormalExamAccessTokens;
import com.guoyu.platform.competition.exam.realtime.FormalExamRealtimePublisher;
import com.guoyu.platform.competition.exam.security.CompetitionExamPrincipal;
import com.guoyu.platform.competition.mapper.CompetitionFormalCandidateExamMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamAccessCommandMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamAccessSessionMapper;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamSessionMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CompetitionFormalExamAccessSessionServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-09T01:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private final CompetitionFormalExamSessionMapper sessions =
      Mockito.mock(CompetitionFormalExamSessionMapper.class);
  private final CompetitionFormalCandidateExamMapper candidateExams =
      Mockito.mock(CompetitionFormalCandidateExamMapper.class);
  private final CompetitionFormalExamAccessSessionMapper accessSessions =
      Mockito.mock(CompetitionFormalExamAccessSessionMapper.class);
  private final CompetitionFormalExamAccessCommandMapper commands =
      Mockito.mock(CompetitionFormalExamAccessCommandMapper.class);
  private final StpLogic examStpLogic = Mockito.mock(StpLogic.class);

  private final CompetitionFormalExamAccessSessionService service =
      new CompetitionFormalExamAccessSessionService(
          sessions, candidateExams, accessSessions, commands, examStpLogic, CLOCK);

  @Test
  void sameDeviceRestoresByReplacingActiveGeneration() {
    stubLoginScope();
    CompetitionFormalExamAccessSessionEntity active = activeAccess(100L, 1, "pad-a", "fp-old");
    when(accessSessions.selectActiveByCandidateExamIdForUpdate(9101L)).thenReturn(List.of(active));
    when(accessSessions.selectMaxGeneration(9101L)).thenReturn(1);
    when(accessSessions.markEndedIfGeneration(
        eq(100L), eq("active"), eq("replaced"), eq(1), eq("same_device_restore"),
        any(LocalDateTime.class), eq(77L))).thenReturn(1);

    CompetitionFormalExamAccessSessionService.IssuedAccess issued =
        service.issueLogin(player(), 9001L, "pad-a");

    assertThat(issued.generation()).isEqualTo(2);
    assertThat(issued.deviceId()).isEqualTo("pad-a");
    verify(examStpLogic).kickout(77L, CompetitionFormalExamAccessTokens.device(100L));
    verify(examStpLogic, never()).kickout(77L);
    ArgumentCaptor<CompetitionFormalExamAccessSessionEntity> inserted =
        ArgumentCaptor.forClass(CompetitionFormalExamAccessSessionEntity.class);
    verify(accessSessions).insert(inserted.capture());
    assertThat(inserted.getValue().getStatus()).isEqualTo("active");
    assertThat(inserted.getValue().getGeneration()).isEqualTo(2);
    ArgumentCaptor<CompetitionFormalExamAccessCommandEntity> command =
        ArgumentCaptor.forClass(CompetitionFormalExamAccessCommandEntity.class);
    verify(commands).insert(command.capture());
    assertThat(command.getValue().getCommandType()).isEqualTo("login_replace");
    assertThat(command.getValue().getReason()).doesNotContain("138");
  }

  @Test
  void otherDeviceIsRejectedWhileActive() {
    stubLoginScope();
    when(accessSessions.selectActiveByCandidateExamIdForUpdate(9101L))
        .thenReturn(List.of(activeAccess(100L, 1, "pad-a", "fp-old")));

    assertThatThrownBy(() -> service.issueLogin(player(), 9001L, "pad-b"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("该选手已有有效考试会话，请联系监考人员")
        .extracting("code")
        .isEqualTo("409");
    verify(accessSessions, never()).insert(any(CompetitionFormalExamAccessSessionEntity.class));
    verify(examStpLogic, never()).kickout(any());
    verify(examStpLogic, never()).kickout(any(), anyString());
  }

  @Test
  void kickedPlayerCanLoginAgainWithNextGeneration() {
    stubLoginScope();
    when(accessSessions.selectActiveByCandidateExamIdForUpdate(9101L)).thenReturn(List.of());
    when(accessSessions.selectMaxGeneration(9101L)).thenReturn(3);

    CompetitionFormalExamAccessSessionService.IssuedAccess issued =
        service.issueLogin(player(), 9001L, "pad-b");

    assertThat(issued.generation()).isEqualTo(4);
    verify(accessSessions).insert(any(CompetitionFormalExamAccessSessionEntity.class));
    verify(examStpLogic, never()).kickout(any(), anyString());
  }

  @Test
  void crossTenantSessionIsNotFound() {
    CompetitionFormalExamSessionEntity session = session();
    session.setTenantId(99L);
    when(sessions.selectById(9001L)).thenReturn(session);

    assertThatThrownBy(() -> service.issueLogin(player(), 9001L, "pad-a"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("正式比赛运行实例不存在")
        .extracting("code")
        .isEqualTo("404");
    verify(candidateExams, never()).selectBySessionAndPlayerId(anyLong(), anyLong());
  }

  @Test
  void forceOfflineCasKicksOnlyThatAccessSession() {
    when(accessSessions.selectActiveByCandidateExamIdForUpdate(9101L))
        .thenReturn(List.of(activeAccess(100L, 2, "pad-a", "fp-old")));
    when(accessSessions.markEndedIfGeneration(
        eq(100L), eq("active"), eq("kicked"), eq(2), eq("工作人员清退"),
        any(LocalDateTime.class), eq(1000L))).thenReturn(1);

    service.forceOffline(9001L, 9101L, 77L, 1000L, "工作人员清退");

    verify(examStpLogic).kickout(77L, CompetitionFormalExamAccessTokens.device(100L));
    verify(examStpLogic, never()).kickout(77L);
    ArgumentCaptor<CompetitionFormalExamAccessCommandEntity> command =
        ArgumentCaptor.forClass(CompetitionFormalExamAccessCommandEntity.class);
    verify(commands).insert(command.capture());
    assertThat(command.getValue().getCommandType()).isEqualTo("force_offline");
    assertThat(command.getValue().getAccessSessionId()).isEqualTo(100L);
    assertThat(command.getValue().getGeneration()).isEqualTo(2);
    assertThat(command.getValue().getStatus()).isEqualTo("issued");
  }

  @Test
  void forceOfflinePublishesKickedRealtimeEvent() {
    FormalExamRealtimePublisher publisher = Mockito.mock(FormalExamRealtimePublisher.class);
    service.setRealtimePublisher(publisher);
    when(accessSessions.selectActiveByCandidateExamIdForUpdate(9101L))
        .thenReturn(List.of(activeAccess(100L, 2, "pad-a", "fp-old")));
    when(accessSessions.markEndedIfGeneration(
        eq(100L), eq("active"), eq("kicked"), eq(2), eq("工作人员清退"),
        any(LocalDateTime.class), eq(1000L))).thenReturn(1);
    Mockito.doAnswer(invocation -> {
      CompetitionFormalExamAccessCommandEntity command = invocation.getArgument(0);
      command.setId(555L);
      return 1;
    }).when(commands).insert(any(CompetitionFormalExamAccessCommandEntity.class));

    service.forceOffline(9001L, 9101L, 77L, 1000L, "工作人员清退");

    verify(publisher).kicked(eq(1L), eq(9001L), eq(9101L), eq(100L), eq(2), eq(555L), any());
  }

  @Test
  void duplicateForceOfflineIsIdempotent() {
    when(accessSessions.selectActiveByCandidateExamIdForUpdate(9101L)).thenReturn(List.of());

    service.forceOffline(9001L, 9101L, 77L, 1000L, "工作人员清退");

    verify(accessSessions, never()).markEndedIfGeneration(
        anyLong(), anyString(), anyString(), anyInt(), anyString(), any(), anyLong());
    verify(commands, never()).insert(any(CompetitionFormalExamAccessCommandEntity.class));
    verify(examStpLogic, never()).kickout(any());
    verify(examStpLogic, never()).kickout(any(), anyString());
  }

  @Test
  void requireActiveByTokenRejectsKickedOrForeignRow() {
    CompetitionFormalExamAccessSessionEntity kicked = activeAccess(100L, 1, "pad-a", "fp");
    kicked.setStatus("kicked");
    when(accessSessions.selectByTokenFingerprint(
        CompetitionFormalExamAccessTokens.fingerprint("old-token"))).thenReturn(kicked);
    assertThat(service.requireActiveByToken("old-token", 1L, 77L)).isNull();

    CompetitionFormalExamAccessSessionEntity otherTenant = activeAccess(101L, 1, "pad-a", "fp2");
    otherTenant.setTenantId(99L);
    when(accessSessions.selectByTokenFingerprint(
        CompetitionFormalExamAccessTokens.fingerprint("other-token"))).thenReturn(otherTenant);
    assertThat(service.requireActiveByToken("other-token", 1L, 77L)).isNull();
  }

  @Test
  void attachTokenStoresFingerprintNotRawToken() {
    when(accessSessions.updateTokenFingerprint(
        eq(55L), eq(CompetitionFormalExamAccessTokens.fingerprint("formal-token")),
        any(LocalDateTime.class), eq(77L))).thenReturn(1);

    service.attachTokenFingerprint(55L, 77L, "formal-token");

    verify(accessSessions).updateTokenFingerprint(
        55L, CompetitionFormalExamAccessTokens.fingerprint("formal-token"),
        LocalDateTime.now(CLOCK), 77L);
  }

  @Test
  void logoutEndsOnlyCurrentAccessSession() {
    CompetitionFormalExamAccessSessionEntity active = activeAccess(100L, 1, "pad-a", "fp-old");
    when(accessSessions.selectActiveByCandidateExamIdForUpdate(9101L)).thenReturn(List.of(active));
    when(accessSessions.markEndedIfGeneration(
        eq(100L), eq("active"), eq("logged_out"), eq(1), eq("player_logout"),
        any(LocalDateTime.class), eq(77L))).thenReturn(1);
    CompetitionExamPrincipal principal =
        new CompetitionExamPrincipal(77L, 1L, 100L, 1, 9001L, 9101L);

    service.logout(principal);

    verify(examStpLogic).kickout(77L, CompetitionFormalExamAccessTokens.device(100L));
    verify(examStpLogic, never()).logout(77L);
  }

  private void stubLoginScope() {
    when(sessions.selectById(9001L)).thenReturn(session());
    when(candidateExams.selectBySessionAndPlayerId(9001L, 77L)).thenReturn(List.of(candidate()));
    when(candidateExams.selectByIdForUpdate(9101L)).thenReturn(candidate());
  }

  private static CompetitionPlayerIdentityEntity player() {
    CompetitionPlayerIdentityEntity player = new CompetitionPlayerIdentityEntity();
    player.setId(77L);
    player.setTenantId(1L);
    return player;
  }

  private static CompetitionFormalExamSessionEntity session() {
    CompetitionFormalExamSessionEntity session = new CompetitionFormalExamSessionEntity();
    session.setId(9001L);
    session.setTenantId(1L);
    session.setStatus("ready");
    return session;
  }

  private static CompetitionFormalCandidateExamEntity candidate() {
    CompetitionFormalCandidateExamEntity entity = new CompetitionFormalCandidateExamEntity();
    entity.setId(9101L);
    entity.setTenantId(1L);
    entity.setExamSessionId(9001L);
    entity.setPlayerId(77L);
    entity.setStatus("waiting_enter");
    return entity;
  }

  private static CompetitionFormalExamAccessSessionEntity activeAccess(
      Long id, int generation, String deviceId, String fingerprint) {
    CompetitionFormalExamAccessSessionEntity entity = new CompetitionFormalExamAccessSessionEntity();
    entity.setId(id);
    entity.setTenantId(1L);
    entity.setExamSessionId(9001L);
    entity.setCandidateExamId(9101L);
    entity.setPlayerId(77L);
    entity.setDeviceId(deviceId);
    entity.setGeneration(generation);
    entity.setStatus("active");
    entity.setTokenFingerprint(fingerprint);
    return entity;
  }
}
