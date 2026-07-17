package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 一键：generate + materialize */
public class TrainShiftAssignGenerateMaterializeReq {

  @NotNull
  private Long planId;
  @NotNull
  private Long trainInfoId;
  @NotNull
  @Min(2000)
  @Max(2100)
  private Integer year;
  @NotNull
  @Min(1)
  @Max(12)
  private Integer month;
  private String adapterType = "T0";
  private Integer classMin;
  private Integer classMax;
  private String mode = "algo_class_as_train_class";
  private Boolean submitForCompanyConfirm = true;
  @Size(max = 80)
  private String classNamePrefix;
  private Boolean onlyUnbound = true;
  private Boolean matchRoster = true;

  public Long getPlanId() { return planId; }
  public void setPlanId(Long planId) { this.planId = planId; }
  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
  public Integer getYear() { return year; }
  public void setYear(Integer year) { this.year = year; }
  public Integer getMonth() { return month; }
  public void setMonth(Integer month) { this.month = month; }
  public String getAdapterType() { return adapterType; }
  public void setAdapterType(String adapterType) { this.adapterType = adapterType; }
  public Integer getClassMin() { return classMin; }
  public void setClassMin(Integer classMin) { this.classMin = classMin; }
  public Integer getClassMax() { return classMax; }
  public void setClassMax(Integer classMax) { this.classMax = classMax; }
  public String getMode() { return mode; }
  public void setMode(String mode) { this.mode = mode; }
  public Boolean getSubmitForCompanyConfirm() { return submitForCompanyConfirm; }
  public void setSubmitForCompanyConfirm(Boolean submitForCompanyConfirm) {
    this.submitForCompanyConfirm = submitForCompanyConfirm;
  }
  public String getClassNamePrefix() { return classNamePrefix; }
  public void setClassNamePrefix(String classNamePrefix) { this.classNamePrefix = classNamePrefix; }
  public Boolean getOnlyUnbound() { return onlyUnbound; }
  public void setOnlyUnbound(Boolean onlyUnbound) { this.onlyUnbound = onlyUnbound; }
  public Boolean getMatchRoster() { return matchRoster; }
  public void setMatchRoster(Boolean matchRoster) { this.matchRoster = matchRoster; }
}
