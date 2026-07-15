package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.entity.ExamFaqConversionEntity;
import java.time.LocalDateTime;

/** FAQ 转换记录返回 VO */
public class FaqConversionVO {

  private Long id;
  private Integer examId;
  private Integer workTypeId;
  private Integer courseId;
  private Integer knowledgeId;
  private String sourceType;
  private Integer sourceDifficulty;
  private String sourceContent;
  private String standardQuestion;
  private String tagName;
  private String conversionStatus;
  private String modelCode;
  private String errorMessage;
  private Integer retryCount;
  private Integer weknoraImported;
  private LocalDateTime weknoraImportedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static FaqConversionVO from(ExamFaqConversionEntity entity) {
    FaqConversionVO vo = new FaqConversionVO();
    vo.setId(entity.getId());
    vo.setExamId(entity.getExamId());
    vo.setWorkTypeId(entity.getWorkTypeId());
    vo.setCourseId(entity.getCourseId());
    vo.setKnowledgeId(entity.getKnowledgeId());
    vo.setSourceType(entity.getSourceType());
    vo.setSourceDifficulty(entity.getSourceDifficulty());
    vo.setSourceContent(entity.getSourceContent());
    vo.setStandardQuestion(entity.getStandardQuestion());
    vo.setTagName(entity.getTagName());
    vo.setConversionStatus(entity.getConversionStatus());
    vo.setModelCode(entity.getModelCode());
    vo.setErrorMessage(entity.getErrorMessage());
    vo.setRetryCount(entity.getRetryCount());
    vo.setWeknoraImported(entity.getWeknoraImported());
    vo.setWeknoraImportedAt(entity.getWeknoraImportedAt());
    vo.setCreatedAt(entity.getCreatedAt());
    vo.setUpdatedAt(entity.getUpdatedAt());
    return vo;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
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
  public String getStandardQuestion() { return standardQuestion; }
  public void setStandardQuestion(String standardQuestion) { this.standardQuestion = standardQuestion; }
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
}
