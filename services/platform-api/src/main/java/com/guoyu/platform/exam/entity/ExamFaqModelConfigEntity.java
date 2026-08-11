package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** FAQ 转换 LLM 模型连接参数配置表 exam_faq_model_config */
@TableName("exam_faq_model_config")
public class ExamFaqModelConfigEntity {

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private String modelCode;
  private String modelName;
  private String baseUrl;
  private String apiKey;
  private String apiFormat;
  private String modelId;
  private BigDecimal temperature;
  private Integer maxTokens;
  /** LongCat 思考模式：enabled / disabled，非 LongCat 模型留空 */
  private String thinking;
  /** DeepSeek 推理强度：low/high/max，默认 high；仅 thinking=enabled 时随 thinking 节点发送 */
  private String reasoningEffort;
  private String extraConfig;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long createdBy;
  private Long updatedBy;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public String getModelCode() { return modelCode; }
  public void setModelCode(String modelCode) { this.modelCode = modelCode; }
  public String getModelName() { return modelName; }
  public void setModelName(String modelName) { this.modelName = modelName; }
  public String getBaseUrl() { return baseUrl; }
  public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
  public String getApiKey() { return apiKey; }
  public void setApiKey(String apiKey) { this.apiKey = apiKey; }
  public String getApiFormat() { return apiFormat; }
  public void setApiFormat(String apiFormat) { this.apiFormat = apiFormat; }
  public String getModelId() { return modelId; }
  public void setModelId(String modelId) { this.modelId = modelId; }
  public BigDecimal getTemperature() { return temperature; }
  public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
  public Integer getMaxTokens() { return maxTokens; }
  public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
  public String getThinking() { return thinking; }
  public void setThinking(String thinking) { this.thinking = thinking; }
  public String getReasoningEffort() { return reasoningEffort; }
  public void setReasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; }
  public String getExtraConfig() { return extraConfig; }
  public void setExtraConfig(String extraConfig) { this.extraConfig = extraConfig; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
