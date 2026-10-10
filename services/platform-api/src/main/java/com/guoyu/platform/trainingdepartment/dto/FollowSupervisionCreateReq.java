package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;

public class FollowSupervisionCreateReq {

  @NotBlank
  private String bizDate;
  private Long scheduleId;
  private String inspectorName;
  private String remark;

  public String getBizDate() {
    return bizDate;
  }

  public void setBizDate(String bizDate) {
    this.bizDate = bizDate;
  }

  public Long getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(Long scheduleId) {
    this.scheduleId = scheduleId;
  }

  public String getInspectorName() {
    return inspectorName;
  }

  public void setInspectorName(String inspectorName) {
    this.inspectorName = inspectorName;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }
}
