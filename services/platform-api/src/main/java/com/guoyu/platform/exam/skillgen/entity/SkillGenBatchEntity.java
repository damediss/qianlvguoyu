package com.guoyu.platform.exam.skillgen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 操作技能试题生成批次 exam_skill_gen_batch */
@TableName("exam_skill_gen_batch")
public class SkillGenBatchEntity {

  public static final String STATUS_PENDING = "pending";
  public static final String STATUS_RUNNING = "running";
  public static final String STATUS_PAUSED = "paused";
  public static final String STATUS_DONE = "done";
  public static final String STATUS_FAILED = "failed";

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private Long blueprintId;
  private Integer workTypeId;
  private Integer level;
  private Long modelConfigId;
  private String modelId;
  private String modelCode;
  private String thinking;
  private String reasoningEffort;
  private String quotaConfig;
  private String status;
  private Integer totalPoints;
  private Integer donePoints;
  private Integer failedPoints;
  private Integer totalQuestions;
  private Long runId;
  private Long tokenUsage;
  private Long promptTokens;
  private Long completionTokens;
  private Long cachedTokens;
  private String promptDigest;
  private String failReason;
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;
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
  public Long getBlueprintId() { return blueprintId; }
  public void setBlueprintId(Long blueprintId) { this.blueprintId = blueprintId; }
  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public Integer getLevel() { return level; }
  public void setLevel(Integer level) { this.level = level; }
  public Long getModelConfigId() { return modelConfigId; }
  public void setModelConfigId(Long modelConfigId) { this.modelConfigId = modelConfigId; }
  public String getModelId() { return modelId; }
  public void setModelId(String modelId) { this.modelId = modelId; }
  public String getModelCode() { return modelCode; }
  public void setModelCode(String modelCode) { this.modelCode = modelCode; }
  public String getThinking() { return thinking; }
  public void setThinking(String thinking) { this.thinking = thinking; }
  public String getReasoningEffort() { return reasoningEffort; }
  public void setReasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; }
  public String getQuotaConfig() { return quotaConfig; }
  public void setQuotaConfig(String quotaConfig) { this.quotaConfig = quotaConfig; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getTotalPoints() { return totalPoints; }
  public void setTotalPoints(Integer totalPoints) { this.totalPoints = totalPoints; }
  public Integer getDonePoints() { return donePoints; }
  public void setDonePoints(Integer donePoints) { this.donePoints = donePoints; }
  public Integer getFailedPoints() { return failedPoints; }
  public void setFailedPoints(Integer failedPoints) { this.failedPoints = failedPoints; }
  public Integer getTotalQuestions() { return totalQuestions; }
  public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
  public Long getRunId() { return runId; }
  public void setRunId(Long runId) { this.runId = runId; }
  public Long getTokenUsage() { return tokenUsage; }
  public void setTokenUsage(Long tokenUsage) { this.tokenUsage = tokenUsage; }
  public Long getPromptTokens() { return promptTokens; }
  public void setPromptTokens(Long promptTokens) { this.promptTokens = promptTokens; }
  public Long getCompletionTokens() { return completionTokens; }
  public void setCompletionTokens(Long completionTokens) { this.completionTokens = completionTokens; }
  public Long getCachedTokens() { return cachedTokens; }
  public void setCachedTokens(Long cachedTokens) { this.cachedTokens = cachedTokens; }
  public String getPromptDigest() { return promptDigest; }
  public void setPromptDigest(String promptDigest) { this.promptDigest = promptDigest; }
  public String getFailReason() { return failReason; }
  public void setFailReason(String failReason) { this.failReason = failReason; }
  public LocalDateTime getStartedAt() { return startedAt; }
  public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
  public LocalDateTime getFinishedAt() { return finishedAt; }
  public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
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
