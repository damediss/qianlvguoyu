package com.guoyu.platform.competition.player.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class CompetitionPlayerSmsSafetyGuardTest {

  @Test
  void acceptsFixedCodeWithFakeSenderOutsideProduction() {
    CompetitionPlayerSmsProperties properties = new CompetitionPlayerSmsProperties();
    properties.setDevelopmentTestCode("123456");

    assertThatCode(() -> CompetitionPlayerSmsSafetyGuard.validate(
        properties, new MockEnvironment().withProperty("spring.profiles.active", "dev")))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsInvalidFixedCodeAtStartup() {
    CompetitionPlayerSmsProperties properties = new CompetitionPlayerSmsProperties();
    properties.setDevelopmentTestCode("1234");

    assertThatThrownBy(() -> CompetitionPlayerSmsSafetyGuard.validate(
        properties, new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("must be six digits");
  }

  @Test
  void rejectsFixedCodeInProduction() {
    CompetitionPlayerSmsProperties properties = new CompetitionPlayerSmsProperties();
    properties.setDevelopmentTestCode("123456");
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");

    assertThatThrownBy(() -> CompetitionPlayerSmsSafetyGuard.validate(properties, environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("forbidden in the prod profile");
  }

  @Test
  void rejectsFakeSenderInProduction() {
    CompetitionPlayerSmsProperties properties = new CompetitionPlayerSmsProperties();
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");

    assertThatThrownBy(() -> CompetitionPlayerSmsSafetyGuard.validate(properties, environment))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("fake sender is forbidden in the prod profile");
  }

  @Test
  void rejectsFixedCodeTogetherWithRealSender() {
    CompetitionPlayerSmsProperties properties = new CompetitionPlayerSmsProperties();
    properties.setDevelopmentTestCode("123456");
    properties.getAliyun().setEnabled(true);

    assertThatThrownBy(() -> CompetitionPlayerSmsSafetyGuard.validate(
        properties, new MockEnvironment()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("cannot be combined with the Aliyun sender");
  }
}
