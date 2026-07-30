package com.guoyu.platform.industrial.dto;

import com.guoyu.platform.common.api.PageQuery;

public class IndustrialTalentServiceQueryReq extends PageQuery {

  private String keyword;
  private String source;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
