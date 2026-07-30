package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** FAQ 转换记录表 exam_faq_conversion */
@TableName("exam_faq_conversion")
public class ExamFaqConversionEntity {

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private Long batchId;
  private Integer examId;
  private Integer workTypeId;
  private Integer courseId;
  private Integer knowledgeId;
  private String sourceType;
  private Integer sourceDifficulty;
  private String sourceContent;
  private String sourceAnswer;
  private String standardQuestion;
  private String answers;
  private String similarQuestions;
  private String negativeQuestions;
  private String tagName;
  private String conversionStatus;
  private String modelCode;
  private String errorMessage;
  private Integer retryCount;
  private Integer weknoraImported;
  private LocalDateTime weknoraImportedAt;
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
  public Integer getExamId() { return examId; }
  public void setExamId(Integer examId) { this.examId = examId; }
  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public Integer getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Integer knowledgeId) { this.knowledgeId = knowledgeId; }
  public String getSourceType() { return sourceType; }
  public void setSourceType(String sourceType) { this.sourceType = sourceType; }
  public Integer getSourceDifficulty() { return sourceDifficulty; }
  public void setSourceDifficulty(Integer sourceDifficulty) { this.sourceDifficulty = sourceDifficulty; }
  public String getSourceContent() { return sourceContent; }
  public void setSourceContent(String sourceContent) { this.sourceContent = sourceContent; }
  public String getSourceAnswer() { return sourceAnswer; }
  public void setSourceAnswer(String sourceAnswer) { this.sourceAnswer = sourceAnswer; }
  public String getStandardQuestion() { return standardQuestion; }
  public void setStandardQuestion(String standardQuestion) { this.standardQuestion = standardQuestion; }
  public String getAnswers() { return answers; }
  public void setAnswers(String answers) { this.answers = answers; }
  public String getSimilarQuestions() { return similarQuestions; }
  public void setSimilarQuestions(String similarQuestions) { this.similarQuestions = similarQuestions; }
  public String getNegativeQuestions() { return negativeQuestions; }
  public void setNegativeQuestions(String negativeQuestions) { this.negativeQuestions = negativeQuestions; }
  public String getTagName() { return tagName; }
  public void setTagName(String tagName) { this.tagName = tagName; }
  public String getConversionStatus() { return conversionStatus; }
  public void setConversionStatus(String conversionStatus) { this.conversionStatus = conversionStatus; }
  public String getModelCode() { return modelCode; }
  public void setModelCode(String modelCode) { this.modelCode = modelCode; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
  public Integer getRetryCount() { return retryCount; }
  public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
  public Integer getWeknoraImported() { return weknoraImported; }
  public void setWeknoraImported(Integer weknoraImported) { this.weknoraImported = weknoraImported; }
  public LocalDateTime getWeknoraImportedAt() { return weknoraImportedAt; }
  public void setWeknoraImportedAt(LocalDateTime weknoraImportedAt) { this.weknoraImportedAt = weknoraImportedAt; }
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
