package com.guoyu.platform.competition.vo;

import com.guoyu.platform.common.api.PageResponse;
import java.time.LocalDateTime;

public class CompetitionRosterImportDetailVO {

  private Long id;
  private Long batchId;
  private Long eventId;
  private Long orgId;
  private String orgName;
  private String batchNo;
  private Long sourceFileId;
  private String status;
  private Integer totalCount;
  private Integer validCount;
  private Integer invalidCount;
  private Long confirmedBy;
  private LocalDateTime confirmedAt;
  private String summary;
  private Long createdBy;
  private LocalDateTime createdAt;
  private PageResponse<CompetitionRosterItemVO> items;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getBatchId() { return batchId; }
  public void setBatchId(Long batchId) { this.batchId = batchId; }
  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public String getBatchNo() { return batchNo; }
  public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
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
  public String getSummary() { return summary; }
  public void setSummary(String summary) { this.summary = summary; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public PageResponse<CompetitionRosterItemVO> getItems() { return items; }
  public void setItems(PageResponse<CompetitionRosterItemVO> items) { this.items = items; }
}
