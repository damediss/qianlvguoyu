package com.guoyu.platform.competition.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.dev33.satoken.stp.StpLogic;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionEventEntity;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.entity.CompetitionRegistrationEntity;
import com.guoyu.platform.competition.enums.CompetitionPlayerSmsPurpose;
import com.guoyu.platform.competition.mapper.CompetitionEventMapper;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerIdentityDomainService;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerDataHash;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerActivationSendReq;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerActivationVerifyReq;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerLoginReq;
import com.guoyu.platform.competition.player.security.CompetitionPlayerPrincipal;
import com.guoyu.platform.competition.player.sms.CompetitionPlayerSmsChallengeResult;
import com.guoyu.platform.competition.player.sms.CompetitionPlayerSmsChallengeService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

class CompetitionPlayerApplicationServiceTest {

  private final CompetitionEventMapper eventMapper = Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionRegistrationMapper registrationMapper = Mockito.mock(CompetitionRegistrationMapper.class);
  private final CompetitionPlayerIdentityMapper playerMapper = Mockito.mock(CompetitionPlayerIdentityMapper.class);
  private final CompetitionPlayerIdentityDomainService playerIdentityService =
      Mockito.mock(CompetitionPlayerIdentityDomainService.class);
  private final CompetitionPlayerDataHash dataHash = Mockito.mock(CompetitionPlayerDataHash.class);
  private final CompetitionPlayerSmsChallengeService smsChallengeService =
      Mockito.mock(CompetitionPlayerSmsChallengeService.class);
  private final StpLogic stpLogic = Mockito.mock(StpLogic.class);
  private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
  private final CompetitionPlayerApplicationService service = new CompetitionPlayerApplicationService(
      eventMapper, registrationMapper, playerMapper, playerIdentityService, dataHash,
      smsChallengeService, stpLogic, passwordEncoder);

  @AfterEach
  void clearTenant() {
    TenantContextHolder.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void sendsActivationCodeOnlyForEligibleUnactivatedRosterRegistration() {
    TenantContextHolder.setTenantId(1L);
    when(eventMapper.selectByPlayerEntryCode("entry-code")).thenReturn(event());
    when(registrationMapper.selectEligibleUnactivatedForPlayerActivation(1L, 1001L, "hash"))
        .thenReturn(registration());
    when(playerMapper.selectForUpdateById(9001L)).thenReturn(player());
    when(dataHash.lookupHash(any())).thenReturn("hash");
    when(smsChallengeService.requestChallenge(any(), any(), any(), any(), any(), any()))
        .thenReturn(new CompetitionPlayerSmsChallengeResult(7001L, LocalDateTime.now().plusMinutes(5)));

    var result = service.sendActivationCode(
        new CompetitionPlayerActivationSendReq("entry-code", "13800138000"), null);

    assertThat(result.challengeId()).isEqualTo(7001L);
    Mockito.verify(smsChallengeService).requestChallenge(
        1L, 9001L, "13800138000", CompetitionPlayerSmsPurpose.ACTIVATION, null, null);
  }

  @Test
  void loginVerifiesPasswordBoundToStoredPlayerAndIssuesPlayerToken() {
    CompetitionPlayerIdentityEntity player = player();
    player.setPasswordHash("{bcrypt}password-hash");
    when(playerIdentityService.normalizeMobile("13800138000")).thenReturn("13800138000");
    when(dataHash.lookupHash("13800138000")).thenReturn("hash");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("hash")).thenReturn(List.of(player));
    when(passwordEncoder.matches("123456", "{bcrypt}password-hash")).thenReturn(true);
    when(stpLogic.getTokenValue()).thenReturn("player-token");
    when(stpLogic.getTokenTimeout()).thenReturn(7200L);

    var result = service.login(new CompetitionPlayerLoginReq("13800138000", "123456"));

    assertThat(result.status()).isEqualTo("active");
    assertThat(result.session().playerId()).isEqualTo(9001L);
    assertThat(result.session().token()).isEqualTo("player-token");
    Mockito.verify(stpLogic).login(9001L);
  }

  @Test
  void loginReturnsPendingActivationWithoutIssuingSession() {
    CompetitionPlayerIdentityEntity player = player();
    player.setStatus("pending_activation");
    player.setPasswordHash("{bcrypt}password-hash");
    when(playerIdentityService.normalizeMobile("13800138000")).thenReturn("13800138000");
    when(dataHash.lookupHash("13800138000")).thenReturn("hash");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("hash")).thenReturn(List.of(player));
    when(passwordEncoder.matches("123456", "{bcrypt}password-hash")).thenReturn(true);

    var result = service.login(new CompetitionPlayerLoginReq("13800138000", "123456"));

    assertThat(result.status()).isEqualTo("pending_activation");
    assertThat(result.session()).isNull();
    Mockito.verify(stpLogic, Mockito.never()).login(any());
  }

  @Test
  void loginLazilyInitializesMissingHistoricalPasswordHash() {
    CompetitionPlayerIdentityEntity player = player();
    player.setStatus("pending_activation");
    player.setIdCardNo("452128199903165016");
    when(playerIdentityService.normalizeMobile("13800138000")).thenReturn("13800138000");
    when(dataHash.lookupHash("13800138000")).thenReturn("hash");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("hash")).thenReturn(List.of(player));
    when(passwordEncoder.encode("165016")).thenReturn("{bcrypt}initial-password-hash");
    when(playerMapper.initializePasswordIfAbsentGlobally(
        Mockito.eq(9001L), Mockito.eq(1L), Mockito.eq("{bcrypt}initial-password-hash"), any()))
        .thenReturn(1);
    when(passwordEncoder.matches("165016", "{bcrypt}initial-password-hash")).thenReturn(true);

    var result = service.login(new CompetitionPlayerLoginReq("13800138000", "165016"));

    assertThat(result.status()).isEqualTo("pending_activation");
    assertThat(player.getPasswordHash()).isEqualTo("{bcrypt}initial-password-hash");
  }

  @Test
  void loginRejectsWrongPasswordWithoutStartingActivation() {
    CompetitionPlayerIdentityEntity player = player();
    player.setPasswordHash("{bcrypt}password-hash");
    when(playerIdentityService.normalizeMobile("13800138000")).thenReturn("13800138000");
    when(dataHash.lookupHash("13800138000")).thenReturn("hash");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("hash")).thenReturn(List.of(player));
    when(passwordEncoder.matches("000000", "{bcrypt}password-hash")).thenReturn(false);

    assertThatThrownBy(() -> service.login(new CompetitionPlayerLoginReq("13800138000", "000000")))
        .hasMessage("账号或密码错误");
    Mockito.verify(stpLogic, Mockito.never()).login(any());
  }

  @Test
  void loginRejectsUnknownAccount() {
    when(playerIdentityService.normalizeMobile("13800138000")).thenReturn("13800138000");
    when(dataHash.lookupHash("13800138000")).thenReturn("hash");
    when(playerMapper.selectForUpdateByMobileLookupHashGlobally("hash")).thenReturn(List.of());

    assertThatThrownBy(() -> service.login(new CompetitionPlayerLoginReq("13800138000", "123456")))
        .hasMessage("账号不存在");
  }

  @Test
  void meUsesNonLockingPlayerLookupInsideReadOnlyTransaction() {
    CompetitionPlayerIdentityEntity player = player();
    CompetitionPlayerPrincipal principal = new CompetitionPlayerPrincipal(9001L, 1L);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    when(playerMapper.selectById(9001L)).thenReturn(player);

    var result = service.me();

    assertThat(result.playerId()).isEqualTo(9001L);
    assertThat(result.status()).isEqualTo("active");
    assertThat(result.maskedMobile()).isEqualTo("138****8000");
    Mockito.verify(playerMapper).selectById(9001L);
    Mockito.verify(playerMapper, Mockito.never()).selectForUpdateById(9001L);
  }

  @Test
  void activationInitializesBcryptPasswordAndIssuesSession() {
    TenantContextHolder.setTenantId(1L);
    CompetitionPlayerIdentityEntity player = player();
    player.setStatus("pending_activation");
    player.setIdCardNo("452128199903165016");
    when(eventMapper.selectByPlayerEntryCode("entry-code")).thenReturn(event());
    when(registrationMapper.selectEligibleUnactivatedForPlayerActivation(1L, 1001L, "hash"))
        .thenReturn(registration());
    when(playerMapper.selectForUpdateById(9001L)).thenReturn(player);
    when(dataHash.lookupHash(any())).thenReturn("hash");
    Mockito.doNothing().when(smsChallengeService).verifyChallenge(9001L, 7001L,
        CompetitionPlayerSmsPurpose.ACTIVATION, "123456");
    when(passwordEncoder.encode("165016")).thenReturn("{bcrypt}initial-password-hash");
    when(playerMapper.updateById(player)).thenReturn(1);
    when(stpLogic.getTokenValue()).thenReturn("player-token");
    when(stpLogic.getTokenTimeout()).thenReturn(7200L);

    var session = service.verifyActivation(new CompetitionPlayerActivationVerifyReq(
        "entry-code", "13800138000", 7001L, "123456"));

    assertThat(player.getStatus()).isEqualTo("active");
    assertThat(player.getPasswordHash()).isEqualTo("{bcrypt}initial-password-hash");
    assertThat(session.token()).isEqualTo("player-token");
    Mockito.verify(stpLogic).login(9001L);
  }

  private CompetitionEventEntity event() {
    CompetitionEventEntity event = new CompetitionEventEntity();
    event.setId(1001L);
    event.setTenantId(1L);
    event.setName("Competition");
    event.setStatus("published");
    return event;
  }

  private CompetitionRegistrationEntity registration() {
    CompetitionRegistrationEntity registration = new CompetitionRegistrationEntity();
    registration.setId(8001L);
    registration.setEventId(1001L);
    registration.setPlayerId(9001L);
    registration.setStatus("eligible_unactivated");
    return registration;
  }

  private CompetitionPlayerIdentityEntity player() {
    CompetitionPlayerIdentityEntity player = new CompetitionPlayerIdentityEntity();
    player.setId(9001L);
    player.setTenantId(1L);
    player.setStatus("active");
    player.setMobile("13800138000");
    return player;
  }
}
