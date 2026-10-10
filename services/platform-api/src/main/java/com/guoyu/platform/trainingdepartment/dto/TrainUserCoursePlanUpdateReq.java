package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 用户课程计划更新请求 */
public class TrainUserCoursePlanUpdateReq {

  @Size(max = 255)
  private String realname;

  @Size(max = 20)
  private String date;

  /** 类型 */
  private Integer type;

  /** 状态 */
  private Integer status;

  /** 排序号 */
  private Integer sortNumber;

  public String getRealname() {
    return realname;
  }

  public void setRealname(String realname) {
    this.realname = realname;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public Integer getType() {
    return type;
  }

  public void setType(Integer type) {
    this.type = type;
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
