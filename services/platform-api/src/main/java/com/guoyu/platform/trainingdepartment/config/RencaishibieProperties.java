package com.guoyu.platform.trainingdepartment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "platform.rencaishibie")
public record RencaishibieProperties(
    boolean enabled,
    String baseUrl,
    String internalToken,
    int connectTimeoutSeconds,
    int readTimeoutSeconds
) {

  public RencaishibieProperties {
    if (baseUrl != null && baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    if (!StringUtils.hasText(baseUrl)) {
      baseUrl = "http://localhost:8001";
    }
    if (connectTimeoutSeconds <= 0) {
      connectTimeoutSeconds = 15;
    }
    if (readTimeoutSeconds <= 0) {
      readTimeoutSeconds = 300;
    }
  }
}
