package com.guoyu.platform.trainingdepartment.dto;

public class FollowSupervisionItemPatchReq {

  /** pass / fail / na */
  private String result;
  private String remark;

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }
}
