package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 培训班级查询请求 */
public class TrainInfoClassQueryReq extends PageQuery {

  /** 开班信息ID */
  private Long trainInfoId;
  private Long companyId;
  private Long workTypeId;
  private Integer companyConfirmStatus;
  /** 班级名称关键字 */
  private String keyword;
  /**
   * 获证筛选：certified=已获证班（有回填真实证书）、uncertified=未获证班、all/空=全部。
   */
  private String certFilter;

  public Long getTrainInfoId() {
    return trainInfoId;
  }

  public void setTrainInfoId(Long trainInfoId) {
    this.trainInfoId = trainInfoId;
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

  public Integer getCompanyConfirmStatus() {
    return companyConfirmStatus;
  }

  public void setCompanyConfirmStatus(Integer companyConfirmStatus) {
    this.companyConfirmStatus = companyConfirmStatus;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getCertFilter() {
    return certFilter;
  }

  public void setCertFilter(String certFilter) {
    this.certFilter = certFilter;
  }
}
