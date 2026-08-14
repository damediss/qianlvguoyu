package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;

public class SubsidyTransitionReq {
  @NotBlank
  private String toPhase;
  private String remark;

  public String getToPhase() { return toPhase; }
  public void setToPhase(String toPhase) { this.toPhase = toPhase; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
