package com.guoyu.platform.exam.zhenti.service;

import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 真题文件签名（方案 §2.6）。
 *
 * <p>算法：sign = HMAC-SHA256(secret, id + "." + t) 十六进制；t 为过期 Unix 秒。
 * 校验 id 与 t 绑定（防 A 签下 B）、过期拒绝、未来时间偏差 &gt;60s 拒绝、常量时间比较。
 */
@Service
@Slf4j
public class ZhentiSignatureService {

  private static final String ALGORITHM = "HmacSHA256";
  private static final long MAX_CLOCK_SKEW_SECONDS = 60;

  private final byte[] secret;
  private final long ttlSeconds;
  private final String rawSecret;

  @org.springframework.beans.factory.annotation.Autowired
  public ZhentiSignatureService(
      @Value("${platform.zhenti.sign-secret:}") String signSecret,
      @Value("${platform.zhenti.sign-ttl-seconds:600}") long ttlSeconds,
      org.springframework.core.env.Environment environment) {
    this(signSecret, ttlSeconds);
    boolean prod = java.util.Arrays.stream(environment.getActiveProfiles())
        .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
    if (prod && rawSecret.startsWith("dev-zhenti")) {
      throw new IllegalStateException(
          "生产环境必须通过 ZHENTI_SIGN_SECRET 注入随机值，禁止使用开发默认密钥");
    }
  }

  /** 供单测直接构造（不校验 profile）。 */
  public ZhentiSignatureService(String signSecret, long ttlSeconds) {
    if (!StringUtils.hasText(signSecret)) {
      throw new IllegalStateException(
          "platform.zhenti.sign-secret 未配置：生产必须通过 ZHENTI_SIGN_SECRET 注入，禁止使用默认值");
    }
    this.rawSecret = signSecret;
    this.secret = signSecret.getBytes(StandardCharsets.UTF_8);
    this.ttlSeconds = ttlSeconds > 0 ? ttlSeconds : 600;
    if (ttlSeconds != 600) {
      log.info("[ZhentiSignature] 签名有效期自定义为 {} 秒", this.ttlSeconds);
    }
  }

  public long ttlSeconds() {
    return ttlSeconds;
  }

  /** 生成签名与过期时间。 */
  public SignedUrl sign(Long id) {
    long expiresAt = Instant.now().getEpochSecond() + ttlSeconds;
    return new SignedUrl(expiresAt, hmac(id, expiresAt));
  }

  /** 指定过期时间签发（供跨时钟/过期场景测试与排障使用）。 */
  public SignedUrl signAt(Long id, long expiresAt) {
    return new SignedUrl(expiresAt, hmac(id, expiresAt));
  }

  /** 校验签名；失败统一 403，不区分原因避免被枚举。 */
  public void verify(Long id, String sign, Long expiresAt) {
    if (id == null || !StringUtils.hasText(sign) || expiresAt == null) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "预览链接无效");
    }
    long now = Instant.now().getEpochSecond();
    if (now > expiresAt) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "预览链接已过期，请重新打开");
    }
    if (expiresAt - now > MAX_CLOCK_SKEW_SECONDS + ttlSeconds) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "预览链接无效");
    }
    byte[] expected = hmac(id, expiresAt).getBytes(StandardCharsets.US_ASCII);
    byte[] actual = sign.trim().getBytes(StandardCharsets.US_ASCII);
    if (!MessageDigest.isEqual(expected, actual)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "预览链接无效");
    }
  }

  String hmac(Long id, long expiresAt) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret, ALGORITHM));
      byte[] digest = mac.doFinal((id + "." + expiresAt).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException | java.security.InvalidKeyException ex) {
      throw new IllegalStateException("HMAC 签名不可用", ex);
    }
  }

  public record SignedUrl(long expiresAt, String sign) {
  }
}
