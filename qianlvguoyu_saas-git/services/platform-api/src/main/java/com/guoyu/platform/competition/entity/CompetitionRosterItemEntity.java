package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("competition_roster_item")
public class CompetitionRosterItemEntity extends BaseEntity {

  private Long batchId;
  private Long playerId;
  private String employeeNo;
  private String name;
  private String workType;
  private String level;
  private String validationStatus;
  private String errorMessage;

  public Long getBatchId() { return batchId; }
  public void setBatchId(Long batchId) { this.batchId = batchId; }
  public Long getPlayerId() { return playerId; }
  public void setPlayerId(Long playerId) { this.playerId = playerId; }
  public String getEmployeeNo() { return employeeNo; }
  public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
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
