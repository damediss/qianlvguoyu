package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.entity.ExamFaqModelConfigEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 模型配置返回 VO（apiKey 脱敏） */
public class FaqModelConfigVO {

  private Long id;
  private String modelCode;
  private String modelName;
  private String baseUrl;
  private String apiKeyMasked;
  private String apiFormat;
  private String modelId;
  private BigDecimal temperature;
  private Integer maxTokens;
  private String extraConfig;
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
    vo.setModelId(entity.getModelId());
    vo.setTemperature(entity.getTemperature());
    vo.setMaxTokens(entity.getMaxTokens());
    vo.setExtraConfig(entity.getExtraConfig());
    vo.setStatus(entity.getStatus());
    vo.setCreatedAt(entity.getCreatedAt());
    vo.setUpdatedAt(entity.getUpdatedAt());
    return vo;
  }

  /** API Key 脱敏：显示前4后4位，中间 *** */
  private static String maskApiKey(String key) {
    if (key == null) return null;
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
  public String getModelId() { return modelId; }
  public void setModelId(String modelId) { this.modelId = modelId; }
  public BigDecimal getTemperature() { return temperature; }
  public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
  public Integer getMaxTokens() { return maxTokens; }
  public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
  public String getExtraConfig() { return extraConfig; }
  public void setExtraConfig(String extraConfig) { this.extraConfig = extraConfig; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
