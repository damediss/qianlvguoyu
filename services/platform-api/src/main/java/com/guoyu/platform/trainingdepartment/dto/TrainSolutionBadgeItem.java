package com.guoyu.platform.trainingdepartment.dto;

import java.math.BigDecimal;

/** 方案经费明细项（保存请求体） */
public class TrainSolutionBadgeItem {

  private String title;
  private BigDecimal badge;
  private BigDecimal expend;

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public BigDecimal getBadge() { return badge; }
  public void setBadge(BigDecimal badge) { this.badge = badge; }
  public BigDecimal getExpend() { return expend; }
  public void setExpend(BigDecimal expend) { this.expend = expend; }
}
