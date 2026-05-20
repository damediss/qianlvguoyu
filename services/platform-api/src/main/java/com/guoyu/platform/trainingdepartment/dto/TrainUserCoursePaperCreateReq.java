package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 用户已获得证书创建请求 */
public class TrainUserCoursePaperCreateReq {

  /** 用户ID */
  private Long userId;

  /** 平台用户ID */
  private Long platformUserId;

  /** 课程ID */
  private Long courseId;

  /** 工种ID */
  private Long workTypeId;

  /** 获取年份 */
  @Size(max = 20)
  private String getYear;

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

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

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
