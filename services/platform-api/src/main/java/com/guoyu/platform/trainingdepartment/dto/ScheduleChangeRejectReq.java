package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 管理端驳回调课申请 */
public class ScheduleChangeRejectReq {

  @Size(max = 500)
  private String rejectReason;

  public String getRejectReason() {
    return rejectReason;
  }

  public void setRejectReason(String rejectReason) {
    this.rejectReason = rejectReason;
  }
}
