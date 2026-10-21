package com.guoyu.platform.competition.player.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.entity.CompetitionPlayerSmsChallengeEntity;
import com.guoyu.platform.competition.enums.CompetitionPlayerSmsChallengeStatus;
import com.guoyu.platform.competition.enums.CompetitionPlayerSmsPurpose;
import com.guoyu.platform.competition.mapper.CompetitionPlayerSmsChallengeMapper;
import com.guoyu.platform.competition.player.config.CompetitionPlayerSmsProperties;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerDataHash;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CompetitionPlayerSmsChallengeServiceTest {

  private static final Long TENANT_ID = 10159L;
  private static final Long PLAYER_ID = 9001L;
  private static final String MOBILE = "18100000000";

  private final CompetitionPlayerSmsChallengeMapper challengeMapper =
      mock(CompetitionPlayerSmsChallengeMapper.class);
  private final CompetitionPlayerSmsRateLimitService rateLimitService =
      mock(CompetitionPlayerSmsRateLimitService.class);
  private final CompetitionPlayerSmsProperties properties = new CompetitionPlayerSmsProperties();
  private final AtomicReference<CompetitionSmsMessage> sentMessage = new AtomicReference<>();
  private final AtomicReference<CompetitionPlayerSmsChallengeEntity> insertedChallenge =
      new AtomicReference<>();
  private final CompetitionSmsSender smsSender = message -> {
    sentMessage.set(message);
    return new CompetitionSmsSendResult(true, "test-provider-message-id");
  };
  private final CompetitionPlayerSmsChallengeService service = new CompetitionPlayerSmsChallengeService(
      challengeMapper,
      new CompetitionPlayerDataHash(),
      rateLimitService,
      smsSender,
      properties);

  @Test
  void sendsRandomSixDigitCodeAndVerifiesTheCodeAcceptedBySender() {
    mockChallengePersistence();

    var result = service.requestChallenge(
        TENANT_ID, PLAYER_ID, MOBILE, CompetitionPlayerSmsPurpose.ACTIVATION, null, null);

    CompetitionSmsMessage message = sentMessage.get();
    CompetitionPlayerSmsChallengeEntity challenge = insertedChallenge.get();
    assertThat(message).isNotNull();
    assertThat(message.mobile()).isEqualTo(MOBILE);
    assertThat(message.code()).matches("\\d{6}");
    assertThat(challenge.getCodeHash()).isNotEqualTo(message.code());
    assertThat(result.challengeId()).isEqualTo(challenge.getId());

    when(challengeMapper.selectForUpdate(
        result.challengeId(), PLAYER_ID, CompetitionPlayerSmsPurpose.ACTIVATION.getCode()))
        .thenReturn(challenge);
    service.verifyChallenge(
        PLAYER_ID, result.challengeId(), CompetitionPlayerSmsPurpose.ACTIVATION, message.code());

    assertThat(challenge.getStatus()).isEqualTo(CompetitionPlayerSmsChallengeStatus.VERIFIED.getCode());
    assertThat(challenge.getConsumedAt()).isNotNull();
  }

  @Test
  void rejectsWrongCodeAndTracksFailedAttempt() {
    properties.setDevelopmentTestCode("123456");
    mockChallengePersistence();

    var result = service.requestChallenge(
        TENANT_ID, PLAYER_ID, MOBILE, CompetitionPlayerSmsPurpose.ACTIVATION, null, null);
    CompetitionPlayerSmsChallengeEntity challenge = insertedChallenge.get();
    when(challengeMapper.selectForUpdate(
        result.challengeId(), PLAYER_ID, CompetitionPlayerSmsPurpose.ACTIVATION.getCode()))
        .thenReturn(challenge);

    assertThatThrownBy(() -> service.verifyChallenge(
        PLAYER_ID, result.challengeId(), CompetitionPlayerSmsPurpose.ACTIVATION, "654321"))
        .hasMessage("验证码错误");

    assertThat(challenge.getFailedAttempts()).isEqualTo(1);
    assertThat(challenge.getStatus()).isEqualTo(CompetitionPlayerSmsChallengeStatus.PENDING.getCode());
  }

  private void mockChallengePersistence() {
    when(challengeMapper.selectActiveLockForUpdate(
        eq(PLAYER_ID), eq(CompetitionPlayerSmsPurpose.ACTIVATION.getCode()), any(LocalDateTime.class)))
        .thenReturn(null);
    when(challengeMapper.insert(any(CompetitionPlayerSmsChallengeEntity.class))).thenAnswer(invocation -> {
      CompetitionPlayerSmsChallengeEntity challenge = invocation.getArgument(0);
      insertedChallenge.set(challenge);
      return 1;
    });
    when(challengeMapper.updateById(any(CompetitionPlayerSmsChallengeEntity.class))).thenReturn(1);
  }
}
