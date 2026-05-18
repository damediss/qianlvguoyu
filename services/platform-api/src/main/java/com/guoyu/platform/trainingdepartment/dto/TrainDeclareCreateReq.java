package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

public class TrainDeclareCreateReq {
  private Long trainId;
  @Size(max = 255) private String title;
  private Long cityId;
  @Size(max = 255) private String cityName;
  private Integer status;
  private Integer sortNumber;

  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Long getCityId() { return cityId; }
  public void setCityId(Long cityId) { this.cityId = cityId; }
  public String getCityName() { return cityName; }
  public void setCityName(String cityName) { this.cityName = cityName; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
