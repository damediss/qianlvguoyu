package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

public class TrainDivisionEnsureTrainInfoReq {

  private Integer planYear;
  private Long workTypeId;
  private Integer workTypeLevel;
  @Size(max = 255)
  private String title;
  /** 默认 true：相同计划+年+工种复用已有培训信息 */
  private Boolean reuseExisting = true;

  public Integer getPlanYear() { return planYear; }
  public void setPlanYear(Integer planYear) { this.planYear = planYear; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public Integer getWorkTypeLevel() { return workTypeLevel; }
  public void setWorkTypeLevel(Integer workTypeLevel) { this.workTypeLevel = workTypeLevel; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Boolean getReuseExisting() { return reuseExisting; }
  public void setReuseExisting(Boolean reuseExisting) { this.reuseExisting = reuseExisting; }
}
