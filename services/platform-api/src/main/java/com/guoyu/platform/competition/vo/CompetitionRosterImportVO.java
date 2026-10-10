package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDateTime;

public class CompetitionRosterImportVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long id;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long batchId;
  private String batchNo;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long orgId;
  private String orgName;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeId;
  private String eventWorkTypeName;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long sourceFileId;
  private String status;
  private Integer totalCount;
  private Integer validCount;
  private Integer invalidCount;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long confirmedBy;
  private LocalDateTime confirmedAt;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long createdBy;
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getBatchId() { return batchId; }
  public void setBatchId(Long batchId) { this.batchId = batchId; }
  public String getBatchNo() { return batchNo; }
  public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public String getEventWorkTypeName() { return eventWorkTypeName; }
  public void setEventWorkTypeName(String eventWorkTypeName) { this.eventWorkTypeName = eventWorkTypeName; }
  public Long getSourceFileId() { return sourceFileId; }
  public void setSourceFileId(Long sourceFileId) { this.sourceFileId = sourceFileId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getTotalCount() { return totalCount; }
  public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
  public Integer getValidCount() { return validCount; }
  public void setValidCount(Integer validCount) { this.validCount = validCount; }
  public Integer getInvalidCount() { return invalidCount; }
  public void setInvalidCount(Integer invalidCount) { this.invalidCount = invalidCount; }
  public Long getConfirmedBy() { return confirmedBy; }
  public void setConfirmedBy(Long confirmedBy) { this.confirmedBy = confirmedBy; }
  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
