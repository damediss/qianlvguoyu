package com.guoyu.platform.certification.dto;

import com.guoyu.platform.common.api.PageQuery;

public class PreparationExamSiteQueryReq extends PageQuery {
  private String keyword;
  private String siteType;
  private String status;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getSiteType() {
    return siteType;
  }

  public void setSiteType(String siteType) {
    this.siteType = siteType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
