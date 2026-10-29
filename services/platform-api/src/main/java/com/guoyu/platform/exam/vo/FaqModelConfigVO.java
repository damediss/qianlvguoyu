package com.guoyu.platform.exam.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 模型配置返回 VO（apiKey 脱敏） */
public class FaqModelConfigVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long id;
  private String modelCode;
  private String modelName;
  private String baseUrl;
  private String apiKeyMasked;
  private String apiFormat;
  private String featureScope;
  private String modelId;
  private BigDecimal temperature;
  private Integer maxTokens;
  private String extraConfig;
  private Integer jevConcurrency;
  private Integer jevReadTimeoutSeconds;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static FaqModelConfigVO from(ExamFaqModelConfigEntity entity) {
    FaqModelConfigVO vo = new FaqModelConfigVO();
    vo.setId(entity.getId());
    vo.setModelCode(entity.getModelCode());
    vo.setModelName(entity.getModelName());
    vo.setBaseUrl(entity.getBaseUrl());
    vo.setApiKeyMasked(maskApiKey(entity.getApiKey()));
    vo.setApiFormat(entity.getApiFormat());
    vo.setFeatureScope(entity.getFeatureScope() == null ? "common" : entity.getFeatureScope());
    vo.setModelId(entity.getModelId());
    vo.setTemperature(entity.getTemperature());
    vo.setMaxTokens(entity.getMaxTokens());
    vo.setExtraConfig(entity.getExtraConfig());
    vo.setJevConcurrency(entity.getJevConcurrency());
    vo.setJevReadTimeoutSeconds(entity.getJevReadTimeoutSeconds());
    vo.setStatus(entity.getStatus());
    vo.setCreatedAt(entity.getCreatedAt());
    vo.setUpdatedAt(entity.getUpdatedAt());
    return vo;
  }

  /** API Key 脱敏：专用 JEV 行的数据库值是说明性占位文案，不是可调用密钥。 */
  private static String maskApiKey(String key) {
    if (key == null) return null;
    if ("模型API Key走配置文件".equals(key)) return "配置文件提供";
    if ("PENDING_USER_INPUT".equals(key)) return "*** 未配置 ***";
    if (key.length() <= 8) return "***";
    return key.substring(0, 4) + "***" + key.substring(key.length() - 4);
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getModelCode() { return modelCode; }
  public void setModelCode(String modelCode) { this.modelCode = modelCode; }
  public String getModelName() { return modelName; }
  public void setModelName(String modelName) { this.modelName = modelName; }
  public String getBaseUrl() { return baseUrl; }
  public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
  public String getApiKeyMasked() { return apiKeyMasked; }
  public void setApiKeyMasked(String apiKeyMasked) { this.apiKeyMasked = apiKeyMasked; }
  public String getApiFormat() { return apiFormat; }
  public void setApiFormat(String apiFormat) { this.apiFormat = apiFormat; }
  public String getFeatureScope() { return featureScope; }
  public void setFeatureScope(String featureScope) { this.featureScope = featureScope; }
  public String getModelId() { return modelId; }
  public void setModelId(String modelId) { this.modelId = modelId; }
  public BigDecimal getTemperature() { return temperature; }
  public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
  public Integer getMaxTokens() { return maxTokens; }
  public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
  public String getExtraConfig() { return extraConfig; }
  public void setExtraConfig(String extraConfig) { this.extraConfig = extraConfig; }
  public Integer getJevConcurrency() { return jevConcurrency; }
  public void setJevConcurrency(Integer jevConcurrency) { this.jevConcurrency = jevConcurrency; }
  public Integer getJevReadTimeoutSeconds() { return jevReadTimeoutSeconds; }
  public void setJevReadTimeoutSeconds(Integer jevReadTimeoutSeconds) {
    this.jevReadTimeoutSeconds = jevReadTimeoutSeconds;
  }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
