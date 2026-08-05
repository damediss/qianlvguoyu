package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_stage_import_batch")
public class CompetitionStageImportBatchEntity extends BaseEntity {
  private Long planId;
  private Long eventId;
  private Long stageId;
  private String batchNo;
  private Long sourceFileId;
  private String operation;
  private String status;
  private Integer totalCount;
  private Integer validCount;
  private Integer invalidCount;
  private String errorMessage;
  private Long confirmedBy;
  private LocalDateTime confirmedAt;

  public Long getPlanId() { return planId; }
  public void setPlanId(Long value) { planId = value; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public String getBatchNo() { return batchNo; }
  public void setBatchNo(String value) { batchNo = value; }
  public Long getSourceFileId() { return sourceFileId; }
  public void setSourceFileId(Long value) { sourceFileId = value; }
  public String getOperation() { return operation; }
  public void setOperation(String value) { operation = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public Integer getTotalCount() { return totalCount; }
  public void setTotalCount(Integer value) { totalCount = value; }
  public Integer getValidCount() { return validCount; }
  public void setValidCount(Integer value) { validCount = value; }
  public Integer getInvalidCount() { return invalidCount; }
  public void setInvalidCount(Integer value) { invalidCount = value; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String value) { errorMessage = value; }
  public Long getConfirmedBy() { return confirmedBy; }
  public void setConfirmedBy(Long value) { confirmedBy = value; }
  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime value) { confirmedAt = value; }
}
