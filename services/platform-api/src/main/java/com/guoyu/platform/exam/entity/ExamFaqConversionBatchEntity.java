package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** FAQ 转换批次表 exam_faq_conversion_batch */
@TableName("exam_faq_conversion_batch")
public class ExamFaqConversionBatchEntity {

  @TableId(type = IdType.INPUT)
  private Long id;

  private Long tenantId;
  private Integer workTypeId;
  private String workTypeName;
  private String courseIds;
  private String modelCode;
  private String modelName;
  private Integer totalExams;
  private Integer successCount;
  private Integer failedCount;
  private Integer skippedCount;
  private Integer pendingCount;
  private String status;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private String errorMessage;
  private Integer concurrencyLimit;
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
  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public String getWorkTypeName() { return workTypeName; }
  public void setWorkTypeName(String workTypeName) { this.workTypeName = workTypeName; }
  public String getCourseIds() { return courseIds; }
  public void setCourseIds(String courseIds) { this.courseIds = courseIds; }
  public String getModelCode() { return modelCode; }
  public void setModelCode(String modelCode) { this.modelCode = modelCode; }
  public String getModelName() { return modelName; }
  public void setModelName(String modelName) { this.modelName = modelName; }
  public Integer getTotalExams() { return totalExams; }
  public void setTotalExams(Integer totalExams) { this.totalExams = totalExams; }
  public Integer getSuccessCount() { return successCount; }
  public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
  public Integer getFailedCount() { return failedCount; }
  public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }
  public Integer getSkippedCount() { return skippedCount; }
  public void setSkippedCount(Integer skippedCount) { this.skippedCount = skippedCount; }
  public Integer getPendingCount() { return pendingCount; }
  public void setPendingCount(Integer pendingCount) { this.pendingCount = pendingCount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getStartedAt() { return startedAt; }
  public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
  public LocalDateTime getCompletedAt() { return completedAt; }
  public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
  public Integer getConcurrencyLimit() { return concurrencyLimit; }
  public void setConcurrencyLimit(Integer concurrencyLimit) { this.concurrencyLimit = concurrencyLimit; }
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
