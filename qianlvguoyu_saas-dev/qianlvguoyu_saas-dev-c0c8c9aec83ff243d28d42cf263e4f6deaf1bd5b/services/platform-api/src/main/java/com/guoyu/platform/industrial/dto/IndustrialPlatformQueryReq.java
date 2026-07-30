package com.guoyu.platform.industrial.dto;

import com.guoyu.platform.common.api.PageQuery;

public class IndustrialPlatformQueryReq extends PageQuery {

  private String keyword;
  private String company;
  private String state;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
