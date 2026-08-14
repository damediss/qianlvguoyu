package com.guoyu.platform.trainingdepartment.dto;

public class FollowSupervisionSubmitReq {

  /** pass / fail / conditional */
  private String conclusion;
  private String remark;

  public String getConclusion() {
    return conclusion;
  }

  public void setConclusion(String conclusion) {
    this.conclusion = conclusion;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }
}
