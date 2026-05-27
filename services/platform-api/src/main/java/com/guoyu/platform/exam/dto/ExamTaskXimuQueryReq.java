package com.guoyu.platform.exam.dto;

import com.guoyu.platform.common.api.PageQuery;

public class ExamTaskXimuQueryReq extends PageQuery {

  private String keyword;

  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
