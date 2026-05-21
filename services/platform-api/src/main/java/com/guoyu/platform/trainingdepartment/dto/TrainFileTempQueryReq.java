package com.guoyu.platform.trainingdepartment.dto;


/** train_file_temp 查询请求 */
public class TrainFileTempQueryReq {

  private String keyword;
  private Integer page = 1;
  private Integer pageSize = 10;
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
