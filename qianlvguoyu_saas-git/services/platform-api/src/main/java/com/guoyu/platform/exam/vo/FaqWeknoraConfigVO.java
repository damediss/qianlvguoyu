package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.entity.ExamFaqWeknoraConfigEntity;
import java.time.LocalDateTime;

/** WeKnora 配置返回 VO */
public class FaqWeknoraConfigVO {

  private Long id;
  private String webhookSecretMasked;
  private String enabledEvents;
  private String knowledgeBaseId;
  private String faqSetId;
  private Integer webhookEnabled;
  private Integer autoExport;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static FaqWeknoraConfigVO from(ExamFaqWeknoraConfigEntity entity) {
    FaqWeknoraConfigVO vo = new FaqWeknoraConfigVO();
    vo.setId(entity.getId());
    vo.setWebhookSecretMasked(maskSecret(entity.getWebhookSecret()));
    vo.setEnabledEvents(entity.getEnabledEvents());
    vo.setKnowledgeBaseId(entity.getKnowledgeBaseId());
    vo.setFaqSetId(entity.getFaqSetId());
    vo.setWebhookEnabled(entity.getWebhookEnabled());
    vo.setAutoExport(entity.getAutoExport());
    vo.setCreatedAt(entity.getCreatedAt());
    vo.setUpdatedAt(entity.getUpdatedAt());
    return vo;
  }

  private static String maskSecret(String secret) {
    if (secret == null) return null;
    if (secret.length() <= 4) return "***";
    return secret.substring(0, 2) + "***" + secret.substring(secret.length() - 2);
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getWebhookSecretMasked() { return webhookSecretMasked; }
  public void setWebhookSecretMasked(String webhookSecretMasked) { this.webhookSecretMasked = webhookSecretMasked; }
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
}
