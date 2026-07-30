package com.guoyu.platform.industrial.dto;

import com.guoyu.platform.common.api.PageQuery;

public class IndustrialCollegeQueryReq extends PageQuery {

  private String keyword;
  private String tag;
  private String status;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
