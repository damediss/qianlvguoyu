package com.guoyu.platform.exam.vo;

import com.guoyu.platform.exam.entity.ExamFaqConversionBatchEntity;
import java.time.LocalDateTime;

/** FAQ 转换批次返回 VO */
public class FaqBatchVO {

  private Long id;
  private Integer workTypeId;
  private String workTypeName;
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
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static FaqBatchVO from(ExamFaqConversionBatchEntity entity) {
    FaqBatchVO vo = new FaqBatchVO();
    vo.setId(entity.getId());
    vo.setWorkTypeId(entity.getWorkTypeId());
    vo.setWorkTypeName(entity.getWorkTypeName());
    vo.setModelCode(entity.getModelCode());
    vo.setModelName(entity.getModelName());
    vo.setTotalExams(entity.getTotalExams());
    vo.setSuccessCount(entity.getSuccessCount());
    vo.setFailedCount(entity.getFailedCount());
    vo.setSkippedCount(entity.getSkippedCount());
    vo.setPendingCount(entity.getPendingCount());
    vo.setStatus(entity.getStatus());
    vo.setStartedAt(entity.getStartedAt());
    vo.setCompletedAt(entity.getCompletedAt());
    vo.setCreatedAt(entity.getCreatedAt());
    vo.setUpdatedAt(entity.getUpdatedAt());
    return vo;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public String getWorkTypeName() { return workTypeName; }
  public void setWorkTypeName(String workTypeName) { this.workTypeName = workTypeName; }
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
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
