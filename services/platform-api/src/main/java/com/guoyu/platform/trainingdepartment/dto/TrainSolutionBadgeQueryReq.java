package com.guoyu.platform.trainingdepartment.dto;

import java.math.BigDecimal;

/** train_solution_badge 查询请求 */
public class TrainSolutionBadgeQueryReq {

  private String keyword;
  private Integer page = 1;
  private Integer pageSize = 10;
  /** 方案ID */
  private Long solutionId;
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getSolutionId() { return solutionId; }
  public void setSolutionId(Long solutionId) { this.solutionId = solutionId; }
}
