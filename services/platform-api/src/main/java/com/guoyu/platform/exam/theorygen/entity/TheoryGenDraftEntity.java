package com.guoyu.platform.exam.theorygen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 理论知识试题生成草稿 exam_theory_gen_draft */
@TableName("exam_theory_gen_draft")
public class TheoryGenDraftEntity {

  public static final String REVIEW_DRAFT = "draft";
  public static final String REVIEW_APPROVED = "approved";
  public static final String REVIEW_REJECTED = "rejected";

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private Long batchId;
  private Long blueprintId;
  private Long nodeId;
  private Long knowledgeId;
  private String questionType;
  private String typeCode;
  private Integer difficulty;
  private Integer consistency;
  private String content;
  private String options;
  private String answer;
  private String analysis;
  private String referenceAnswer;
  private String scorePoints;
  private String scoringStandard;
  private BigDecimal score;
  private String aiModel;
  private String aiThinking;
  private Integer genDurationMs;
  private String reviewStatus;
  private Long reviewBy;
  private LocalDateTime reviewAt;
  private Integer publishStatus;
  private String publishedTaskType;
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
  public Long getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Long knowledgeId) { this.knowledgeId = knowledgeId; }
  public String getQuestionType() { return questionType; }
  public void setQuestionType(String questionType) { this.questionType = questionType; }
  public String getTypeCode() { return typeCode; }
  public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
  public Integer getDifficulty() { return difficulty; }
  public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
  public Integer getConsistency() { return consistency; }
  public void setConsistency(Integer consistency) { this.consistency = consistency; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getOptions() { return options; }
  public void setOptions(String options) { this.options = options; }
  public String getAnswer() { return answer; }
  public void setAnswer(String answer) { this.answer = answer; }
  public String getAnalysis() { return analysis; }
  public void setAnalysis(String analysis) { this.analysis = analysis; }
  public String getReferenceAnswer() { return referenceAnswer; }
  public void setReferenceAnswer(String referenceAnswer) { this.referenceAnswer = referenceAnswer; }
  public String getScorePoints() { return scorePoints; }
  public void setScorePoints(String scorePoints) { this.scorePoints = scorePoints; }
  public String getScoringStandard() { return scoringStandard; }
  public void setScoringStandard(String scoringStandard) { this.scoringStandard = scoringStandard; }
  public BigDecimal getScore() { return score; }
  public void setScore(BigDecimal score) { this.score = score; }
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
  public String getPublishedTaskType() { return publishedTaskType; }
  public void setPublishedTaskType(String publishedTaskType) { this.publishedTaskType = publishedTaskType; }
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
