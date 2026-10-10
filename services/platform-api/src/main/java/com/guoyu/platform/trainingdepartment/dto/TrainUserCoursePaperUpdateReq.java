package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 用户已获得证书更新请求 */
public class TrainUserCoursePaperUpdateReq {

  /** 工种ID */
  private Long workTypeId;

  /** 获取年份 */
  @Size(max = 20)
  private String getYear;

  /** 状态 */
  private Integer status;

  /** 排序号 */
  private Integer sortNumber;

  public Long getWorkTypeId() {
    return workTypeId;
  }

  public void setWorkTypeId(Long workTypeId) {
    this.workTypeId = workTypeId;
  }

  public String getGetYear() {
    return getYear;
  }

  public void setGetYear(String getYear) {
    this.getYear = getYear;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
