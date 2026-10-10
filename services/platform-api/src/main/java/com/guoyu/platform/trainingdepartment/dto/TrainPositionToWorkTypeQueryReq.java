package com.guoyu.platform.trainingdepartment.dto;


/** train_position_to_work_type 查询请求 */
public class TrainPositionToWorkTypeQueryReq {

  private String keyword;
  private Integer page = 1;
  private Integer pageSize = 10;
  /** 工种ID */
  private Long workTypeId;
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
}
