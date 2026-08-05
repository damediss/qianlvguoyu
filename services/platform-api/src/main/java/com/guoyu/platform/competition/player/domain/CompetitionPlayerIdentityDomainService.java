package com.guoyu.platform.competition.player.domain;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.enums.CompetitionPlayerStatus;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionPlayerIdentityDomainService {

  private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
  private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[0-9X]$");
  private static final int[] ID_CARD_WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
  private static final char[] ID_CARD_CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

  private final CompetitionPlayerIdentityMapper playerMapper;
  private final CompetitionPlayerSensitiveDataCrypto crypto;
  private final PasswordEncoder passwordEncoder;

  public CompetitionPlayerIdentityDomainService(
      CompetitionPlayerIdentityMapper playerMapper,
      CompetitionPlayerSensitiveDataCrypto crypto,
      PasswordEncoder passwordEncoder) {
    this.playerMapper = playerMapper;
    this.crypto = crypto;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public CompetitionPlayerIdentityEntity resolveOrCreate(
      Long tenantId, String idCardNo, String mobile) {
    if (tenantId == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "缺少租户信息");
    }
    String normalizedIdCard = normalizeIdCard(idCardNo);
    String normalizedMobile = normalizeMobile(mobile);
    String idCardHash = crypto.lookupHash(normalizedIdCard);
    String mobileHash = crypto.lookupHash(normalizedMobile);
    List<CompetitionPlayerIdentityEntity> matches = playerMapper.selectForUpdateByIdentityHashes(
        idCardHash, mobileHash);
    if (matches == null) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "选手身份查询失败");
    }
    if (matches.size() == 1) {
      CompetitionPlayerIdentityEntity match = matches.getFirst();
      if (Objects.equals(match.getIdCardLookupHash(), idCardHash)
          && Objects.equals(match.getMobileLookupHash(), mobileHash)) {
        return match;
      }
      throw identityConflict();
    }
    if (matches.size() > 1) {
      throw identityConflict();
    }

    CompetitionPlayerIdentityEntity player = new CompetitionPlayerIdentityEntity();
    player.setTenantId(tenantId);
    player.setIdCardCiphertext(crypto.encrypt(normalizedIdCard));
    player.setIdCardLookupHash(idCardHash);
    player.setMobileCiphertext(crypto.encrypt(normalizedMobile));
    player.setMobileLookupHash(mobileHash);
    player.setCryptoKeyVersion(crypto.keyVersion());
    player.setPasswordHash(passwordEncoder.encode(initialPassword(normalizedIdCard)));
    player.setPasswordInitializedAt(java.time.LocalDateTime.now());
    player.setStatus(CompetitionPlayerStatus.PENDING_ACTIVATION.getCode());
    player.setDeleted(0);
    try {
      int inserted = playerMapper.insert(player);
      if (inserted != 1) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "选手身份创建失败");
      }
      return player;
    } catch (DuplicateKeyException ex) {
      List<CompetitionPlayerIdentityEntity> concurrentMatches = playerMapper.selectForUpdateByIdentityHashes(
          idCardHash, mobileHash);
      if (concurrentMatches != null && concurrentMatches.size() == 1) {
        CompetitionPlayerIdentityEntity match = concurrentMatches.getFirst();
        if (Objects.equals(match.getIdCardLookupHash(), idCardHash)
            && Objects.equals(match.getMobileLookupHash(), mobileHash)) {
          return match;
        }
      }
      throw identityConflict();
    }
  }

  public String normalizeMobile(String mobile) {
    String normalized = mobile == null ? "" : mobile.trim().replace(" ", "");
    if (!MOBILE_PATTERN.matcher(normalized).matches()) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "手机号格式不合法");
    }
    return normalized;
  }

  public String normalizeIdCard(String idCardNo) {
    String normalized = idCardNo == null ? "" : idCardNo.trim().toUpperCase(Locale.ROOT).replace(" ", "");
    if (!ID_CARD_PATTERN.matcher(normalized).matches() || !hasValidIdCardCheckCode(normalized)) {
      throw new BusinessException(ErrorCode.UNPROCESSABLE_ENTITY, "身份证号格式不合法");
    }
    return normalized;
  }

  @Transactional
  public CompetitionPlayerIdentityEntity changeMobile(Long playerId, String newMobile) {
    String normalizedMobile = normalizeMobile(newMobile);
    CompetitionPlayerIdentityEntity player = playerMapper.selectForUpdateById(playerId);
    if (player == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手身份不存在");
    }
    String newMobileHash = assertMobileAvailable(player.getId(), normalizedMobile);
    player.setMobileCiphertext(crypto.encrypt(normalizedMobile));
    player.setMobileLookupHash(newMobileHash);
    player.setCryptoKeyVersion(crypto.keyVersion());
    try {
      if (playerMapper.updateById(player) != 1) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "手机号更新失败");
      }
    } catch (DuplicateKeyException ex) {
      throw new BusinessException(ErrorCode.CONFLICT, "手机号已绑定其他选手");
    }
    return player;
  }

  @Transactional
  public CompetitionPlayerIdentityEntity updateSensitiveData(
      Long tenantId, Long playerId, String idCardNo, String mobile) {
    CompetitionPlayerIdentityEntity player = playerMapper.selectForUpdateById(playerId);
    if (player == null || !Objects.equals(tenantId, player.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手身份不存在");
    }
    String normalizedIdCard = normalizeIdCard(idCardNo);
    String normalizedMobile = normalizeMobile(mobile);
    String idCardHash = crypto.lookupHash(normalizedIdCard);
    String mobileHash = crypto.lookupHash(normalizedMobile);
    List<CompetitionPlayerIdentityEntity> matches = playerMapper.selectForUpdateByIdentityHashes(
        idCardHash, mobileHash);
    if (matches != null) {
      for (CompetitionPlayerIdentityEntity match : matches) {
        if (!Objects.equals(match.getId(), playerId)) {
          throw identityConflict();
        }
      }
    }
    player.setIdCardCiphertext(crypto.encrypt(normalizedIdCard));
    player.setIdCardLookupHash(idCardHash);
    player.setMobileCiphertext(crypto.encrypt(normalizedMobile));
    player.setMobileLookupHash(mobileHash);
    player.setCryptoKeyVersion(crypto.keyVersion());
    try {
      if (playerMapper.updateById(player) != 1) {
        throw new BusinessException(ErrorCode.CONFLICT, "选手身份更新失败，请刷新后重试");
      }
    } catch (DuplicateKeyException ex) {
      throw identityConflict();
    }
    return player;
  }

  @Transactional(readOnly = true)
  public CompetitionPlayerIdentityEntity resolveExisting(Long tenantId, Long playerId) {
    CompetitionPlayerIdentityEntity player = playerMapper.selectById(playerId);
    if (player == null || !Objects.equals(tenantId, player.getTenantId())) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "选手身份不存在");
    }
    return player;
  }

  @Transactional(readOnly = true)
  public String assertMobileAvailable(Long playerId, String newMobile) {
    String normalizedMobile = normalizeMobile(newMobile);
    String newMobileHash = crypto.lookupHash(normalizedMobile);
    CompetitionPlayerIdentityEntity bound = playerMapper.selectForUpdateByMobileLookupHash(newMobileHash);
    if (bound != null && !Objects.equals(bound.getId(), playerId)) {
      throw new BusinessException(ErrorCode.CONFLICT, "手机号已绑定其他选手");
    }
    return newMobileHash;
  }

  private String initialPassword(String idCardNo) {
    if (idCardNo == null || idCardNo.length() < 6) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Initial competition player password cannot be generated");
    }
    return idCardNo.substring(idCardNo.length() - 6);
  }

  private boolean hasValidIdCardCheckCode(String idCardNo) {
    int sum = 0;
    for (int i = 0; i < ID_CARD_WEIGHTS.length; i++) {
      sum += (idCardNo.charAt(i) - '0') * ID_CARD_WEIGHTS[i];
    }
    return ID_CARD_CHECK_CODES[sum % 11] == idCardNo.charAt(17);
  }

  private BusinessException identityConflict() {
    return new BusinessException(ErrorCode.CONFLICT, "身份证与手机号已分别绑定其他选手");
  }
}
