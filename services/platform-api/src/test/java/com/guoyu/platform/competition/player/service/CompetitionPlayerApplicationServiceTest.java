package com.guoyu.platform.competition.player.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.guoyu.platform.competition.player.domain.CompetitionPlayerSensitiveDataCrypto;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerActivationSendReq;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerActivationVerifyReq;
import com.guoyu.platform.competition.player.dto.CompetitionPlayerLoginReq;
import com.guoyu.platform.competition.player.sms.CompetitionPlayerSmsChallengeResult;
import com.guoyu.platform.competition.player.sms.CompetitionPlayerSmsChallengeService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

class CompetitionPlayerApplicationServiceTest {

  private final CompetitionEventMapper eventMapper = Mockito.mock(CompetitionEventMapper.class);
  private final CompetitionRegistrationMapper registrationMapper = Mockito.mock(CompetitionRegistrationMapper.class);
  private final CompetitionPlayerIdentityMapper playerMapper = Mockito.mock(CompetitionPlayerIdentityMapper.class);
  private final CompetitionPlayerIdentityDomainService playerIdentityService =
      Mockito.mock(CompetitionPlayerIdentityDomainService.class);
  private final CompetitionPlayerSensitiveDataCrypto crypto = Mockito.mock(CompetitionPlayerSensitiveDataCrypto.class);
  private final CompetitionPlayerSmsChallengeService smsChallengeService =
      Mockito.mock(CompetitionPlayerSmsChallengeService.class);
  private final StpLogic stpLogic = Mockito.mock(StpLogic.class);
  private final PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
  private final CompetitionPlayerApplicationService service = new CompetitionPlayerApplicationService(
      eventMapper, registrationMapper, playerMapper, playerIdentityService, crypto,
      smsChallengeService, stpLogic, passwordEncoder);

  @AfterEach
  void clearTenant() {
    TenantContextHolder.clear();
  }

  @Test
  void sendsActivationCodeOnlyForEligibleUnactivatedRosterRegistration() {
    TenantContextHolder.setTenantId(1L);
    when(eventMapper.selectByPlayerEntryCode("entry-code")).thenReturn(event());
    when(registrationMapper.selectEligibleUnactivatedForPlayerActivation(1L, 1001L, "hash"))
        .thenReturn(registration());
    when(playerMapper.selectForUpdateById(9001L)).thenReturn(player());
    when(crypto.decrypt(any())).thenReturn("13800138000");
    when(crypto.lookupHash(any())).thenReturn("hash");
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
    TenantContextHolder.setTenantId(1L);
    CompetitionPlayerIdentityEntity player = player();
    player.setPasswordHash("{bcrypt}password-hash");
    when(playerIdentityService.normalizeMobile("13800138000")).thenReturn("13800138000");
    when(crypto.lookupHash("13800138000")).thenReturn("hash");
    when(playerMapper.selectOne(any())).thenReturn(player);
    when(passwordEncoder.matches("123456", "{bcrypt}password-hash")).thenReturn(true);
    when(stpLogic.getTokenValue()).thenReturn("player-token");
    when(stpLogic.getTokenTimeout()).thenReturn(7200L);

    var result = service.login(new CompetitionPlayerLoginReq("13800138000", "123456"));

    assertThat(result.playerId()).isEqualTo(9001L);
    assertThat(result.token()).isEqualTo("player-token");
    Mockito.verify(stpLogic).login(9001L);
  }

  @Test
  void activationInitializesBcryptPasswordFromLastSixIdCardDigitsWithoutIssuingSession() {
    TenantContextHolder.setTenantId(1L);
    CompetitionPlayerIdentityEntity player = player();
    player.setStatus("pending_activation");
    player.setIdCardCiphertext("id-card-ciphertext");
    when(eventMapper.selectByPlayerEntryCode("entry-code")).thenReturn(event());
    when(registrationMapper.selectEligibleUnactivatedForPlayerActivation(1L, 1001L, "hash"))
        .thenReturn(registration());
    when(playerMapper.selectForUpdateById(9001L)).thenReturn(player);
    when(crypto.lookupHash(any())).thenReturn("hash");
    Mockito.doNothing().when(smsChallengeService).verifyChallenge(9001L, 7001L,
        CompetitionPlayerSmsPurpose.ACTIVATION, "123456");
    when(crypto.decrypt("id-card-ciphertext")).thenReturn("452128199903165016");
    when(passwordEncoder.encode("165016")).thenReturn("{bcrypt}initial-password-hash");
    when(playerMapper.updateById(player)).thenReturn(1);

    service.verifyActivation(new CompetitionPlayerActivationVerifyReq(
        "entry-code", "13800138000", 7001L, "123456"));

    assertThat(player.getStatus()).isEqualTo("active");
    assertThat(player.getPasswordHash()).isEqualTo("{bcrypt}initial-password-hash");
    Mockito.verify(stpLogic, Mockito.never()).login(any());
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
    player.setMobileCiphertext("ciphertext");
    return player;
  }
}
