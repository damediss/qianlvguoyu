package com.guoyu.platform.trainingdepartment.dto;

/** platform_user 查询 */
public class PlatformUserQueryReq {
  private String keyword;
  private Integer page = 1;
  private Integer pageSize = 20;
  private Long companyId;

  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
}
