package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 开班信息查询请求 */
public class TrainInfoListQueryReq extends PageQuery {

  /** 搜索关键词 */
  private String keyword;
  /** 状态 */
  private Integer status;
  private Long companyId;
  private Long workTypeId;
  /** 课程ID */
  private Long courseId;
  /** 审核状态 */
  private Integer passStatus;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }

  public Long getWorkTypeId() {
    return workTypeId;
  }

  public void setWorkTypeId(Long workTypeId) {
    this.workTypeId = workTypeId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Integer getPassStatus() {
    return passStatus;
  }

  public void setPassStatus(Integer passStatus) {
    this.passStatus = passStatus;
  }
}
