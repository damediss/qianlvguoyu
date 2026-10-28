package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 师资库查询请求 */
public class TrainTeacherBankQueryReq extends PageQuery {

  /** 搜索关键词 */
  private String keyword;
  /** 状态 */
  private Integer status;
  /** 学历 */
  private String education;
  /** 归属企业组织 ID（机构/超管筛选；企业账号由 Scope 强制） */
  private Long companyId;

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

  public String getEducation() {
    return education;
  }

  public void setEducation(String education) {
    this.education = education;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }
}
