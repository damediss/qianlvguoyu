package com.guoyu.platform.exam.dto;

import com.guoyu.platform.common.api.PageQuery;

public class ExpertTaskQueryReq extends PageQuery {

  private String keyword;
  private Integer status;

  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
}
