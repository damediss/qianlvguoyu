package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_simulation_sync_batch")
public class CompetitionSimulationSyncBatchEntity extends BaseEntity {
  private Long bindingId;
  private Long eventId;
  private Long stageId;
  private Long eventWorkTypeId;
  private Long eventWorkTypeLevelId;
  private String batchStatus;
  private Integer totalRows;
  private Integer validRows;
  private Integer errorRows;
  private Integer confirmedRows;
  private String correctionReason;
  private String errorMessage;
  private LocalDateTime confirmedAt;
  private Long confirmedBy;

  public Long getBindingId() { return bindingId; }
  public void setBindingId(Long value) { bindingId = value; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long value) { eventId = value; }
  public Long getStageId() { return stageId; }
  public void setStageId(Long value) { stageId = value; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long value) { eventWorkTypeId = value; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long value) { eventWorkTypeLevelId = value; }
  public String getBatchStatus() { return batchStatus; }
  public void setBatchStatus(String value) { batchStatus = value; }
  public Integer getTotalRows() { return totalRows; }
  public void setTotalRows(Integer value) { totalRows = value; }
  public Integer getValidRows() { return validRows; }
  public void setValidRows(Integer value) { validRows = value; }
  public Integer getErrorRows() { return errorRows; }
  public void setErrorRows(Integer value) { errorRows = value; }
  public Integer getConfirmedRows() { return confirmedRows; }
  public void setConfirmedRows(Integer value) { confirmedRows = value; }
  public String getCorrectionReason() { return correctionReason; }
  public void setCorrectionReason(String value) { correctionReason = value; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String value) { errorMessage = value; }
  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime value) { confirmedAt = value; }
  public Long getConfirmedBy() { return confirmedBy; }
  public void setConfirmedBy(Long value) { confirmedBy = value; }
}
