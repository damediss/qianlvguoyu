package com.guoyu.platform.exam.zhenti.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guoyu.platform.common.exception.BusinessException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZhentiSignatureServiceTest {

  private final ZhentiSignatureService service =
      new ZhentiSignatureService("unit-test-secret", 600);

  @Test
  @DisplayName("正常签发 → 校验通过")
  void signAndVerify() {
    var signed = service.sign(123L);
    assertThatCode(() -> service.verify(123L, signed.sign(), signed.expiresAt()))
        .doesNotThrowAnyException();
    assertThat(signed.expiresAt()).isGreaterThan(Instant.now().getEpochSecond());
  }

  @Test
  @DisplayName("篡改 id（A 签下 B）→ 403")
  void tamperedIdRejected() {
    var signed = service.sign(123L);
    assertThatThrownBy(() -> service.verify(124L, signed.sign(), signed.expiresAt()))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("403"));
  }

  @Test
  @DisplayName("篡改过期时间 → 403")
  void tamperedExpiryRejected() {
    var signed = service.sign(123L);
    assertThatThrownBy(() -> service.verify(123L, signed.sign(), signed.expiresAt() + 1))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("已过期 → 403")
  void expiredRejected() {
    var signed = service.signAt(123L, Instant.now().getEpochSecond() - 1);
    assertThatThrownBy(() -> service.verify(123L, signed.sign(), signed.expiresAt()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("过期");
  }

  @Test
  @DisplayName("换 secret 签发的签名 → 403")
  void wrongSecretRejected() {
    var other = new ZhentiSignatureService("another-secret", 600);
    var signed = other.sign(123L);
    assertThatThrownBy(() -> service.verify(123L, signed.sign(), signed.expiresAt()))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("缺 sign / 缺 t → 403")
  void missingParametersRejected() {
    assertThatThrownBy(() -> service.verify(123L, null, 1L))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> service.verify(123L, "abc", null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("空 secret 启动失败（生产禁止默认值）")
  void blankSecretRejected() {
    assertThatThrownBy(() -> new ZhentiSignatureService("", 600))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("prod 使用开发默认密钥 → 启动失败")
  void prodDefaultSecretRejected() {
    var environment = new org.springframework.mock.env.MockEnvironment();
    environment.setActiveProfiles("prod");
    assertThatThrownBy(() -> new ZhentiSignatureService(
        "dev-zhenti-sign-secret-please-change", 600, environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ZHENTI_SIGN_SECRET");
  }
}
