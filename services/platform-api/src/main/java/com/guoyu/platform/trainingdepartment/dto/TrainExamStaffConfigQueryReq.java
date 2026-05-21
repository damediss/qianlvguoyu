package com.guoyu.platform.trainingdepartment.dto;


/** train_exam_staff_config 查询请求 */
public class TrainExamStaffConfigQueryReq {

  private String keyword;
  private Integer page = 1;
  private Integer pageSize = 10;
  /** 用户ID */
  private Long userId;
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
}
