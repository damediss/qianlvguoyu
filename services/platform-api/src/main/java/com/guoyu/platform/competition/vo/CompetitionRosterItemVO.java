package com.guoyu.platform.competition.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public class CompetitionRosterItemVO {

  @JsonSerialize(using = ToStringSerializer.class)
  private Long id;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long batchId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long orgId;
  private String orgName;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeId;
  @JsonSerialize(using = ToStringSerializer.class)
  private Long eventWorkTypeLevelId;
  private Integer sourceRowNo;
  private String name;
  private String workType;
  private String level;
  private String validationStatus;
  private String errorMessage;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getBatchId() { return batchId; }
  public void setBatchId(Long batchId) { this.batchId = batchId; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public Long getEventWorkTypeId() { return eventWorkTypeId; }
  public void setEventWorkTypeId(Long eventWorkTypeId) { this.eventWorkTypeId = eventWorkTypeId; }
  public Long getEventWorkTypeLevelId() { return eventWorkTypeLevelId; }
  public void setEventWorkTypeLevelId(Long eventWorkTypeLevelId) { this.eventWorkTypeLevelId = eventWorkTypeLevelId; }
  public Integer getSourceRowNo() { return sourceRowNo; }
  public void setSourceRowNo(Integer sourceRowNo) { this.sourceRowNo = sourceRowNo; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getWorkType() { return workType; }
  public void setWorkType(String workType) { this.workType = workType; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public String getValidationStatus() { return validationStatus; }
  public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
