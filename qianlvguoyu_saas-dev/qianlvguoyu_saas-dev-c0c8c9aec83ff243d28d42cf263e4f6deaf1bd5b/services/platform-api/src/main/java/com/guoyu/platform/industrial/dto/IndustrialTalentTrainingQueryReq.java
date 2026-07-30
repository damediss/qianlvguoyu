package com.guoyu.platform.industrial.dto;

import com.guoyu.platform.common.api.PageQuery;

public class IndustrialTalentTrainingQueryReq extends PageQuery {

  private String keyword;
  private String workType;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getWorkType() {
    return workType;
  }

  public void setWorkType(String workType) {
    this.workType = workType;
  }
}
