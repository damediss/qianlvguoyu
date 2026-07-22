package com.guoyu.platform.competition.player.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.competition.player.config.CompetitionPlayerCryptoProperties;
import org.junit.jupiter.api.Test;

class CompetitionPlayerSensitiveDataCryptoTest {

  @Test
  void encryptsWithAesGcmAndBuildsDeterministicLookupHash() {
    CompetitionPlayerCryptoProperties properties = new CompetitionPlayerCryptoProperties();
    properties.setAesKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    properties.setLookupPepper("test-only-pepper");
    properties.setKeyVersion("v1");
    CompetitionPlayerSensitiveDataCrypto crypto = new CompetitionPlayerSensitiveDataCrypto(properties);

    String encrypted = crypto.encrypt("13800138000");

    assertThat(encrypted).startsWith("v1.");
    assertThat(crypto.decrypt(encrypted)).isEqualTo("13800138000");
    assertThat(crypto.lookupHash("13800138000")).hasSize(64)
        .isEqualTo(crypto.lookupHash("13800138000"));
  }
}
