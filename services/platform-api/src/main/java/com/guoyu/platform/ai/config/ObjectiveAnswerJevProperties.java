package com.guoyu.platform.ai.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 客观题答案检查专用 JEV 配置（方案 §9.1）。只在 ai 包绑定，业务模块不可见。
 *
 * <p>不得写入批次 JSON、调用审计、异常信息或日志。
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "platform.objectiveanswercheck.jev")
public class ObjectiveAnswerJevProperties {

  private String baseUrl = "https://tokendance.space/gateway";
  private String model = "ateve-jev-v1";
  private String apiKey = "";

  @Min(5)
  @Max(120)
  private int readTimeoutSeconds = 30;

  public String baseUrlOrNull() {
    return baseUrl == null || baseUrl.isBlank() ? null : baseUrl.trim();
  }

  public String apiKeyOrNull() {
    return apiKey == null || apiKey.isBlank() ? null : apiKey.trim();
  }
}
