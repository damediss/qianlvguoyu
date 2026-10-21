package com.guoyu.platform.competition.player.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Fails closed when a deterministic or fake SMS configuration could reach production. */
@Component
@Slf4j
public class CompetitionPlayerSmsSafetyGuard {

  public CompetitionPlayerSmsSafetyGuard(
      CompetitionPlayerSmsProperties properties,
      Environment environment) {
    validate(properties, environment);
  }

  static void validate(CompetitionPlayerSmsProperties properties, Environment environment) {
    String testCode = properties.getDevelopmentTestCode();
    boolean hasTestCode = testCode != null && !testCode.isBlank();
    boolean aliyunEnabled = properties.getAliyun().isEnabled();

    if (hasTestCode && !testCode.matches("\\d{6}")) {
      throw new IllegalStateException(
          "platform.competition.player.sms.development-test-code must be six digits");
    }
    if (hasTestCode && aliyunEnabled) {
      throw new IllegalStateException(
          "Competition player SMS test code cannot be combined with the Aliyun sender");
    }
    if (environment.acceptsProfiles(Profiles.of("prod"))) {
      if (hasTestCode) {
        throw new IllegalStateException(
            "Competition player SMS test code is forbidden in the prod profile");
      }
      if (!aliyunEnabled) {
        throw new IllegalStateException(
            "Competition player SMS fake sender is forbidden in the prod profile");
      }
    }
    if (hasTestCode) {
      log.warn("Competition player deterministic SMS test code is enabled outside production");
    }
  }
}
