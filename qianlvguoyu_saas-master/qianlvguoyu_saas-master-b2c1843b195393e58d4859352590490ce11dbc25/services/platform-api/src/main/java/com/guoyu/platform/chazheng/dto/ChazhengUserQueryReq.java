package com.guoyu.platform.chazheng.dto;

import com.guoyu.platform.common.api.PageQuery;

public class ChazhengUserQueryReq extends PageQuery {

  private String keyword;
  private Integer enterpriseId;
  private String ymd;

  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getEnterpriseId() { return enterpriseId; }
  public void setEnterpriseId(Integer enterpriseId) { this.enterpriseId = enterpriseId; }
  public String getYmd() { return ymd; }
  public void setYmd(String ymd) { this.ymd = ymd; }
}