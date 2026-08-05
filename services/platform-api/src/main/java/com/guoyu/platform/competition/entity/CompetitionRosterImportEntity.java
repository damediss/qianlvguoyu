package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

@TableName("competition_roster_import")
public class CompetitionRosterImportEntity extends BaseEntity {

  private Long eventId;
  private Long orgId;
  private Long eventWorkTypeId;
  private String batchNo;
  private Long sourceFileId;
  private String status;
  private Integer totalCount;
  private Integer validCount;
  private Integer invalidCount;
  private Long confirmedBy;
  private LocalDateTime confirmedAt;
  private String summary;

  public Long getEventId() { return eventId; }
  public void setEventId(Long eventId) { this.eventId = eventId; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
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
}
