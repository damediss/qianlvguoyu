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
  /** 负责人确认：0=待办，-1=已办（不筛选） */
  private Integer headConfirm;
  /** 组类型 */
  private Integer groupType;
  /** 排除的组类型（不等于） */
  private Integer groupTypeNe;
  /** 分类标签（精确匹配） */
  private String cate;

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

  public Integer getHeadConfirm() {
    return headConfirm;
  }

  public void setHeadConfirm(Integer headConfirm) {
    this.headConfirm = headConfirm;
  }

  public Integer getGroupType() {
    return groupType;
  }

  public void setGroupType(Integer groupType) {
    this.groupType = groupType;
  }

  public Integer getGroupTypeNe() {
    return groupTypeNe;
  }

  public void setGroupTypeNe(Integer groupTypeNe) {
    this.groupTypeNe = groupTypeNe;
  }

  public String getCate() {
    return cate;
  }

  public void setCate(String cate) {
    this.cate = cate;
  }
}
