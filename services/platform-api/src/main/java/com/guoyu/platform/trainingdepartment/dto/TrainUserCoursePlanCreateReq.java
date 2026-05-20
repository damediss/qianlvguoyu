package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 用户课程计划创建请求 */
public class TrainUserCoursePlanCreateReq {

  /** 用户ID */
  private Long userId;

  /** 平台用户ID */
  private Long platformUserId;

  @Size(max = 255)
  private String realname;

  /** 添加来源 */
  private Integer addFrom;

  /** 课程ID */
  private Long courseId;

  @Size(max = 20)
  private String date;

  /** 类型 */
  private Integer type;

  /** 状态 */
  private Integer status;

  /** 排序号 */
  private Integer sortNumber;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getPlatformUserId() {
    return platformUserId;
  }

  public void setPlatformUserId(Long platformUserId) {
    this.platformUserId = platformUserId;
  }

  public String getRealname() {
    return realname;
  }

  public void setRealname(String realname) {
    this.realname = realname;
  }

  public Integer getAddFrom() {
    return addFrom;
  }

  public void setAddFrom(Integer addFrom) {
    this.addFrom = addFrom;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
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
