package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

public class TrainRequestQueryReq extends PageQuery {
  private Long companyId;
  private String keyword;

  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
