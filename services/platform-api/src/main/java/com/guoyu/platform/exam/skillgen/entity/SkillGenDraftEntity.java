package com.guoyu.platform.exam.skillgen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 操作技能试题生成草稿 exam_skill_gen_draft */
@TableName("exam_skill_gen_draft")
public class SkillGenDraftEntity {

  public static final String REVIEW_DRAFT = "draft";
  public static final String REVIEW_APPROVED = "approved";
  public static final String REVIEW_REJECTED = "rejected";

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private Long batchId;
  private Long blueprintId;
  private Long nodeId;
  private String questionName;
  private BigDecimal score;
  private BigDecimal weight;
  private Integer durationMinutes;
  private String examForm;
  private String sitePreparation;
  private String examineePrepare;
  private String requirements;
  private String negatives;
  private String scoringItems;
  private Integer difficulty;
  private Integer consistency;
  private String aiModel;
  private String aiThinking;
  private Integer genDurationMs;
  private String reviewStatus;
  private Long reviewBy;
  private LocalDateTime reviewAt;
  private Integer publishStatus;
  private Long publishedTaskId;
  private Long publishedDetailId;
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
  public Long getBatchId() { return batchId; }
  public void setBatchId(Long batchId) { this.batchId = batchId; }
  public Long getBlueprintId() { return blueprintId; }
  public void setBlueprintId(Long blueprintId) { this.blueprintId = blueprintId; }
  public Long getNodeId() { return nodeId; }
  public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
  public String getQuestionName() { return questionName; }
  public void setQuestionName(String questionName) { this.questionName = questionName; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal score) { this.score = score; }
  public BigDecimal getWeight() { return weight; }
  public void setWeight(BigDecimal weight) { this.weight = weight; }
  public Integer getDurationMinutes() { return durationMinutes; }
  public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
  public String getExamForm() { return examForm; }
  public void setExamForm(String examForm) { this.examForm = examForm; }
  public String getSitePreparation() { return sitePreparation; }
  public void setSitePreparation(String sitePreparation) { this.sitePreparation = sitePreparation; }
  public String getExamineePrepare() { return examineePrepare; }
  public void setExamineePrepare(String examineePrepare) { this.examineePrepare = examineePrepare; }
  public String getRequirements() { return requirements; }
  public void setRequirements(String requirements) { this.requirements = requirements; }
  public String getNegatives() { return negatives; }
  public void setNegatives(String negatives) { this.negatives = negatives; }
  public String getScoringItems() { return scoringItems; }
  public void setScoringItems(String scoringItems) { this.scoringItems = scoringItems; }
  public Integer getDifficulty() { return difficulty; }
  public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
  public Integer getConsistency() { return consistency; }
  public void setConsistency(Integer consistency) { this.consistency = consistency; }
  public String getAiModel() { return aiModel; }
  public void setAiModel(String aiModel) { this.aiModel = aiModel; }
  public String getAiThinking() { return aiThinking; }
  public void setAiThinking(String aiThinking) { this.aiThinking = aiThinking; }
  public Integer getGenDurationMs() { return genDurationMs; }
  public void setGenDurationMs(Integer genDurationMs) { this.genDurationMs = genDurationMs; }
  public String getReviewStatus() { return reviewStatus; }
  public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
  public Long getReviewBy() { return reviewBy; }
  public void setReviewBy(Long reviewBy) { this.reviewBy = reviewBy; }
  public LocalDateTime getReviewAt() { return reviewAt; }
  public void setReviewAt(LocalDateTime reviewAt) { this.reviewAt = reviewAt; }
  public Integer getPublishStatus() { return publishStatus; }
  public void setPublishStatus(Integer publishStatus) { this.publishStatus = publishStatus; }
  public Long getPublishedTaskId() { return publishedTaskId; }
  public void setPublishedTaskId(Long publishedTaskId) { this.publishedTaskId = publishedTaskId; }
  public Long getPublishedDetailId() { return publishedDetailId; }
  public void setPublishedDetailId(Long publishedDetailId) { this.publishedDetailId = publishedDetailId; }
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
