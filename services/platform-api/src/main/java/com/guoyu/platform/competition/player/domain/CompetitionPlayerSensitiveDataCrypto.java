package com.guoyu.platform.competition.player.domain;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.player.config.CompetitionPlayerCryptoProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** Encrypts player PII for storage and creates keyed hashes for exact lookup only. */
@Component
public class CompetitionPlayerSensitiveDataCrypto {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int GCM_NONCE_BYTES = 12;
  private static final int GCM_TAG_BITS = 128;
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

  private final CompetitionPlayerCryptoProperties properties;

  public CompetitionPlayerSensitiveDataCrypto(CompetitionPlayerCryptoProperties properties) {
    this.properties = properties;
  }

  public String encrypt(String plainText) {
    try {
      byte[] nonce = new byte[GCM_NONCE_BYTES];
      SECURE_RANDOM.nextBytes(nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, aesKey(), new GCMParameterSpec(GCM_TAG_BITS, nonce));
      byte[] ciphertext = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      return keyVersion() + "." + URL_ENCODER.encodeToString(nonce) + "."
          + URL_ENCODER.encodeToString(ciphertext);
    } catch (GeneralSecurityException ex) {
      throw unavailable(ex);
    }
  }

  public String decrypt(String encryptedValue) {
    try {
      String[] parts = encryptedValue == null ? new String[0] : encryptedValue.split("\\.", -1);
      if (parts.length != 3 || !parts[0].equals(keyVersion())) {
        throw unavailable(null);
      }
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, aesKey(), new GCMParameterSpec(
          GCM_TAG_BITS, URL_DECODER.decode(parts[1])));
      return new String(cipher.doFinal(URL_DECODER.decode(parts[2])), StandardCharsets.UTF_8);
    } catch (BusinessException ex) {
      throw ex;
    } catch (IllegalArgumentException | GeneralSecurityException ex) {
      throw unavailable(ex);
    }
  }

  public String lookupHash(String normalizedValue) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(lookupPepper().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return toHex(mac.doFinal(normalizedValue.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException ex) {
      throw unavailable(ex);
    }
  }

  public String keyVersion() {
    if (properties.getKeyVersion() == null || properties.getKeyVersion().isBlank()) {
      throw unavailable(null);
    }
    return properties.getKeyVersion().trim();
  }

  private SecretKey aesKey() {
    try {
      byte[] decoded = Base64.getDecoder().decode(properties.getAesKeyBase64());
      if (decoded.length != 16 && decoded.length != 24 && decoded.length != 32) {
        throw unavailable(null);
      }
      return new SecretKeySpec(decoded, "AES");
    } catch (IllegalArgumentException ex) {
      throw unavailable(ex);
    }
  }

  private String lookupPepper() {
    if (properties.getLookupPepper() == null || properties.getLookupPepper().isBlank()) {
      throw unavailable(null);
    }
    return properties.getLookupPepper();
  }

  private String toHex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) {
      builder.append(String.format("%02x", value));
    }
    return builder.toString();
  }

  private BusinessException unavailable(Exception cause) {
    return new BusinessException(ErrorCode.INTERNAL_ERROR, "选手敏感信息加密配置不可用");
  }
}
