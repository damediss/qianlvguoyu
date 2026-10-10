package com.guoyu.platform.certification.dto;

import com.guoyu.platform.common.api.PageQuery;

public class PreparationAssessorQueryReq extends PageQuery {
  private String keyword;
  private String status;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
