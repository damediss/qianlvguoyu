package com.guoyu.platform.industrial.dto;

import com.guoyu.platform.common.api.PageQuery;

public class ProjectResourceQueryReq extends PageQuery {

  private String keyword;
  private String state;
  private String source;
  private String company;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }
}
