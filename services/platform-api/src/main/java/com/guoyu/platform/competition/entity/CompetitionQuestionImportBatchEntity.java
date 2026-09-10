package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_question_import_batch")
public class CompetitionQuestionImportBatchEntity extends BaseEntity {

  private Long eventId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String contentType;
  private String originalFilename;
  private String tempFilePath;
  private String status;
  private Integer totalCount;
  private Integer successCount;
  private Integer failureCount;
  private String errorDetail;
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
  public String getOriginalFilename() { return originalFilename; }
  public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
  public String getTempFilePath() { return tempFilePath; }
  public void setTempFilePath(String tempFilePath) { this.tempFilePath = tempFilePath; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getTotalCount() { return totalCount; }
  public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
  public Integer getSuccessCount() { return successCount; }
  public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
  public Integer getFailureCount() { return failureCount; }
  public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
  public String getErrorDetail() { return errorDetail; }
  public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }
  public LocalDateTime getStartedAt() { return startedAt; }
  public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
  public LocalDateTime getFinishedAt() { return finishedAt; }
  public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
