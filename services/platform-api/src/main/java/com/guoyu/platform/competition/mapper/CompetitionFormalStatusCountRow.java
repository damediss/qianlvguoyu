package com.guoyu.platform.competition.mapper;

public class CompetitionFormalStatusCountRow {

  private String status;
  private Long total;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getTotal() {
    return total;
  }

  public void setTotal(Long total) {
    this.total = total;
  }

  public int totalOrZero() {
    return total == null ? 0 : total.intValue();
  }
}
