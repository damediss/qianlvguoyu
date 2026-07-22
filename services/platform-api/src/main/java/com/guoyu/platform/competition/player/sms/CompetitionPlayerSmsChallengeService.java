package com.guoyu.platform.competition.player.sms;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionPlayerSmsChallengeEntity;
import com.guoyu.platform.competition.enums.CompetitionPlayerSmsChallengeStatus;
import com.guoyu.platform.competition.enums.CompetitionPlayerSmsDeliveryStatus;
import com.guoyu.platform.competition.enums.CompetitionPlayerSmsPurpose;
import com.guoyu.platform.competition.mapper.CompetitionPlayerSmsChallengeMapper;
import com.guoyu.platform.competition.player.config.CompetitionPlayerSmsProperties;
import com.guoyu.platform.competition.player.domain.CompetitionPlayerSensitiveDataCrypto;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionPlayerSmsChallengeService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final CompetitionPlayerSmsChallengeMapper challengeMapper;
  private final CompetitionPlayerSensitiveDataCrypto crypto;
  private final CompetitionPlayerSmsRateLimitService rateLimitService;
  private final CompetitionSmsSender smsSender;
  private final CompetitionPlayerSmsProperties properties;

  public CompetitionPlayerSmsChallengeService(
      CompetitionPlayerSmsChallengeMapper challengeMapper,
      CompetitionPlayerSensitiveDataCrypto crypto,
      CompetitionPlayerSmsRateLimitService rateLimitService,
      CompetitionSmsSender smsSender,
      CompetitionPlayerSmsProperties properties) {
    this.challengeMapper = challengeMapper;
    this.crypto = crypto;
    this.rateLimitService = rateLimitService;
    this.smsSender = smsSender;
    this.properties = properties;
  }

  @Transactional
  public CompetitionPlayerSmsChallengeResult requestChallenge(
      Long tenantId,
      Long playerId,
      String normalizedMobile,
      CompetitionPlayerSmsPurpose purpose,
      String requestIpHash,
      String requestDeviceHash) {
    if (tenantId == null || playerId == null || purpose == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码请求参数不完整");
    }
    LocalDateTime now = LocalDateTime.now();
    CompetitionPlayerSmsChallengeEntity activeLock = challengeMapper.selectActiveLockForUpdate(
        playerId, purpose.getCode(), now);
    if (activeLock != null) {
      throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "验证码校验已锁定，请稍后再试");
    }
    String mobileLookupHash = crypto.lookupHash(normalizedMobile);
    rateLimitService.acquire(tenantId, playerId, mobileLookupHash, purpose);

    CompetitionPlayerSmsChallengeEntity challenge = new CompetitionPlayerSmsChallengeEntity();
    challenge.setId(IdWorker.getId());
    challenge.setTenantId(tenantId);
    challenge.setPlayerId(playerId);
    challenge.setPurpose(purpose.getCode());
    challenge.setMobileLookupHash(mobileLookupHash);
    String code = sixDigitCode();
    challenge.setCodeHash(codeHash(challenge.getId(), code));
    challenge.setStatus(CompetitionPlayerSmsChallengeStatus.PENDING.getCode());
    challenge.setFailedAttempts(0);
    challenge.setExpiresAt(now.plusSeconds(properties.getCodeTtlSeconds()));
    challenge.setSentAt(now);
    challenge.setDeliveryStatus(CompetitionPlayerSmsDeliveryStatus.PENDING.getCode());
    challenge.setRequestIpHash(requestIpHash);
    challenge.setRequestDeviceHash(requestDeviceHash);
    challenge.setDeleted(0);
    if (challengeMapper.insert(challenge) != 1) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "验证码创建失败");
    }

    CompetitionSmsSendResult delivery = smsSender.send(new CompetitionSmsMessage(
        normalizedMobile, purpose, code));
    if (delivery == null || !delivery.accepted()) {
      challenge.setDeliveryStatus(CompetitionPlayerSmsDeliveryStatus.FAILED.getCode());
      challengeMapper.updateById(challenge);
      log.warn("Competition player SMS was not accepted by provider, tenantId={}, playerId={}, purpose={}",
          tenantId, playerId, purpose.getCode());
      throw new BusinessException(ErrorCode.BAD_GATEWAY, "验证码发送失败，请稍后再试");
    }
    challenge.setDeliveryStatus(CompetitionPlayerSmsDeliveryStatus.ACCEPTED.getCode());
    challenge.setProviderMessageId(delivery.providerMessageId());
    challengeMapper.updateById(challenge);
    return new CompetitionPlayerSmsChallengeResult(challenge.getId(), challenge.getExpiresAt());
  }

  @Transactional
  public void verifyChallenge(
      Long playerId,
      Long challengeId,
      CompetitionPlayerSmsPurpose purpose,
      String code) {
    if (playerId == null || challengeId == null || purpose == null || code == null || code.isBlank()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "验证码参数不完整");
    }
    CompetitionPlayerSmsChallengeEntity challenge = challengeMapper.selectForUpdate(
        challengeId, playerId, purpose.getCode());
    if (challenge == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "验证码不存在");
    }
    LocalDateTime now = LocalDateTime.now();
    if (CompetitionPlayerSmsChallengeStatus.LOCKED.getCode().equals(challenge.getStatus())
        && challenge.getLockedUntil() != null && challenge.getLockedUntil().isAfter(now)) {
      throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "验证码校验已锁定，请稍后再试");
    }
    if (!CompetitionPlayerSmsChallengeStatus.PENDING.getCode().equals(challenge.getStatus())) {
      throw new BusinessException(ErrorCode.CONFLICT, "验证码已失效");
    }
    if (!challenge.getExpiresAt().isAfter(now)) {
      challenge.setStatus(CompetitionPlayerSmsChallengeStatus.EXPIRED.getCode());
      challengeMapper.updateById(challenge);
      throw new BusinessException(ErrorCode.CONFLICT, "验证码已过期");
    }
    boolean verified = MessageDigest.isEqual(
        challenge.getCodeHash().getBytes(StandardCharsets.UTF_8),
        codeHash(challenge.getId(), code.trim()).getBytes(StandardCharsets.UTF_8));
    if (verified) {
      challenge.setStatus(CompetitionPlayerSmsChallengeStatus.VERIFIED.getCode());
      challenge.setConsumedAt(now);
      challengeMapper.updateById(challenge);
      return;
    }
    int failures = challenge.getFailedAttempts() == null ? 1 : challenge.getFailedAttempts() + 1;
    challenge.setFailedAttempts(failures);
    if (failures >= properties.getMaxFailedAttempts()) {
      challenge.setStatus(CompetitionPlayerSmsChallengeStatus.LOCKED.getCode());
      challenge.setLockedUntil(now.plusSeconds(properties.getLockSeconds()));
      challengeMapper.updateById(challenge);
      throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "验证码校验失败次数过多，请稍后再试");
    }
    challengeMapper.updateById(challenge);
    throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "验证码错误");
  }

  private String sixDigitCode() {
    return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
  }

  private String codeHash(Long challengeId, String code) {
    return crypto.lookupHash("competition-player-sms:" + challengeId + ":" + code);
  }
}
