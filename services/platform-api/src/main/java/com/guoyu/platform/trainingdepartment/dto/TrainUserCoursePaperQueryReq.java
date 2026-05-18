package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 用户已获得证书查询请求 */
public class TrainUserCoursePaperQueryReq extends PageQuery {

  private Long userId;
  private Long courseId;
  /** 工种ID */
  private Long workTypeId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
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
}
