package com.guoyu.platform.chazheng.dto;

import com.guoyu.platform.common.api.PageQuery;

public class ChazhengUserQueryReq extends PageQuery {

  private String keyword;
  private Integer enterpriseId;
  private String ymd;
  /** 查证状态过滤：0=未成功（未查证或查证失败，可重查），1=已查证成功 */
  private Integer isChecked;

  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getEnterpriseId() { return enterpriseId; }
  public void setEnterpriseId(Integer enterpriseId) { this.enterpriseId = enterpriseId; }
  public String getYmd() { return ymd; }
  public void setYmd(String ymd) { this.ymd = ymd; }
  public Integer getIsChecked() { return isChecked; }
  public void setIsChecked(Integer isChecked) { this.isChecked = isChecked; }
}