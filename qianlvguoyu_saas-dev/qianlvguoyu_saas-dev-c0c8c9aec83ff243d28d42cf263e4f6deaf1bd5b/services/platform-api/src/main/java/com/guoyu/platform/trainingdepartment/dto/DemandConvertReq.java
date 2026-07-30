package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 需求转培训开班请求 */
public class DemandConvertReq {

  @NotBlank
  private String title;

  @NotNull
  private Long companyId;

  private String trainCompanyName;

  @NotNull
  private Long workTypeId;

  @NotNull
  private Integer workTypeLevel;

  @NotNull
  private Integer num;

  /** 关联的需求ID */
  private Long traindemandId;

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
  public String getTrainCompanyName() { return trainCompanyName; }
  public void setTrainCompanyName(String trainCompanyName) { this.trainCompanyName = trainCompanyName; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public Integer getWorkTypeLevel() { return workTypeLevel; }
  public void setWorkTypeLevel(Integer workTypeLevel) { this.workTypeLevel = workTypeLevel; }
  public Integer getNum() { return num; }
  public void setNum(Integer num) { this.num = num; }
  public Long getTraindemandId() { return traindemandId; }
  public void setTraindemandId(Long traindemandId) { this.traindemandId = traindemandId; }
}
