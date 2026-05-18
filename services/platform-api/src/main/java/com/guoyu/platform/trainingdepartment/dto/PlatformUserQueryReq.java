package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 平台用户查询请求 */
public class PlatformUserQueryReq extends PageQuery {

  /** 关键字(匹配姓名或手机号) */
  private String keyword;

  /** 企业ID */
  private Long companyId;

  /** 机构ID */
  private Long orgId;

  /** 状态 */
  private String stat;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public void setCompanyId(Long companyId) {
    this.companyId = companyId;
  }

  public Long getOrgId() {
    return orgId;
  }

  public void setOrgId(Long orgId) {
    this.orgId = orgId;
  }

  public String getStat() {
    return stat;
  }

  public void setStat(String stat) {
    this.stat = stat;
  }
}
