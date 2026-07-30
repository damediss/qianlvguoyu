package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;

public class TrainDivisionRosterParseReq {

  @NotNull
  private Long planId;
  private Long trainInfoId;
  /** enterprise_roster | plan_item */
  private String mode = "enterprise_roster";
  private Boolean fallbackToPlanItem = true;
  private Integer planYear;
  private String jobName;

  public Long getPlanId() { return planId; }
  public void setPlanId(Long planId) { this.planId = planId; }
  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
  public String getMode() { return mode; }
  public void setMode(String mode) { this.mode = mode; }
  public Boolean getFallbackToPlanItem() { return fallbackToPlanItem; }
  public void setFallbackToPlanItem(Boolean fallbackToPlanItem) { this.fallbackToPlanItem = fallbackToPlanItem; }
  public Integer getPlanYear() { return planYear; }
  public void setPlanYear(Integer planYear) { this.planYear = planYear; }
  public String getJobName() { return jobName; }
  public void setJobName(String jobName) { this.jobName = jobName; }
}
