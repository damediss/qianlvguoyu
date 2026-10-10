package com.guoyu.platform.trainingdepartment.dto;


/** train_task_log 查询请求 */
public class TrainTaskLogQueryReq {

  private String keyword;
  private Integer page = 1;
  private Integer pageSize = 10;
  /** 班级ID */
  private Long trainClassId;
  /** 任务检查点ID */
  private Long ckId;
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getTrainClassId() { return trainClassId; }
  public void setTrainClassId(Long trainClassId) { this.trainClassId = trainClassId; }
  public Long getCkId() { return ckId; }
  public void setCkId(Long ckId) { this.ckId = ckId; }
}
