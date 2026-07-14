package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** WeKnora 集成配置表 exam_faq_weknora_config */
@TableName("exam_faq_weknora_config")
public class ExamFaqWeknoraConfigEntity {

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private String webhookSecret;
  private String enabledEvents;
  private String knowledgeBaseId;
  private String faqSetId;
  private Integer webhookEnabled;
  private Integer autoExport;
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
  public String getWebhookSecret() { return webhookSecret; }
  public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
  public String getEnabledEvents() { return enabledEvents; }
  public void setEnabledEvents(String enabledEvents) { this.enabledEvents = enabledEvents; }
  public String getKnowledgeBaseId() { return knowledgeBaseId; }
  public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
  public String getFaqSetId() { return faqSetId; }
  public void setFaqSetId(String faqSetId) { this.faqSetId = faqSetId; }
  public Integer getWebhookEnabled() { return webhookEnabled; }
  public void setWebhookEnabled(Integer webhookEnabled) { this.webhookEnabled = webhookEnabled; }
  public Integer getAutoExport() { return autoExport; }
  public void setAutoExport(Integer autoExport) { this.autoExport = autoExport; }
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
