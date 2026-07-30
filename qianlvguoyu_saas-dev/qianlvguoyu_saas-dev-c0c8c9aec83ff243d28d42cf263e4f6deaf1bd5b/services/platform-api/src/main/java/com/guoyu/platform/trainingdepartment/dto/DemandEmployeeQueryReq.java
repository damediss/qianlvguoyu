package com.guoyu.platform.trainingdepartment.dto;


/** demand_employee 查询请求 */
public class DemandEmployeeQueryReq {

  private String keyword;
  private Integer page = 1;
  private Integer pageSize = 10;
  /** 企业ID */
  private Long orgId;
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
}
