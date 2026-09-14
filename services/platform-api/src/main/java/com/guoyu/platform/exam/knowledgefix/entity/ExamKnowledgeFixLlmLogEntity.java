package com.guoyu.platform.exam.knowledgefix.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 知识归属修正 LLM 调用审计（LLM 规范 #10） */
@TableName("exam_knowledge_fix_llm_log")
public class ExamKnowledgeFixLlmLogEntity {

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private Long batchNo;
  private Integer detailId;
  private String stage;
  private String modelId;
  private String promptDigest;
  private Integer promptTokens;
  private Integer completionTokens;
  private Integer totalTokens;
  private Integer cachedTokens;
  private Integer durationMs;
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
  public Long getBatchNo() { return batchNo; }
  public void setBatchNo(Long batchNo) { this.batchNo = batchNo; }
  public Integer getDetailId() { return detailId; }
  public void setDetailId(Integer detailId) { this.detailId = detailId; }
  public String getStage() { return stage; }
  public void setStage(String stage) { this.stage = stage; }
  public String getModelId() { return modelId; }
  public void setModelId(String modelId) { this.modelId = modelId; }
  public String getPromptDigest() { return promptDigest; }
  public void setPromptDigest(String promptDigest) { this.promptDigest = promptDigest; }
  public Integer getPromptTokens() { return promptTokens; }
  public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
  public Integer getCompletionTokens() { return completionTokens; }
  public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
  public Integer getTotalTokens() { return totalTokens; }
  public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
  public Integer getCachedTokens() { return cachedTokens; }
  public void setCachedTokens(Integer cachedTokens) { this.cachedTokens = cachedTokens; }
  public Integer getDurationMs() { return durationMs; }
  public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
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
