package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class TrainShiftAssignGenerateReq {

  @NotNull
  private Long planId;
  private Long trainInfoId;
  @NotNull
  @Min(2000)
  @Max(2100)
  private Integer year;
  @NotNull
  @Min(1)
  @Max(12)
  private Integer month;
  /** T0 直通（标准人日）；三期再扩企业 Adapter */
  private String adapterType = "T0";
  private Integer classMin;
  private Integer classMax;

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
}
