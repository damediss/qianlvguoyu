package com.guoyu.platform.trainingdepartment.dto;

/**
 * 培训补贴查询请求
 */
public class TrainSubsidyQueryReq {
  private Integer page = 1;
  private Integer pageSize = 20;
  /** 项目ID */
  private Long projectId;
  private Integer materialStatus;
  private Integer rssbStatus;
  /** 状态 */
  private Integer status;
  /** 搜索关键词 */
  private String keyword;

  public Integer getPage() { return page; }
  public void setPage(Integer page) { this.page = page; }
  public Integer getPageSize() { return pageSize; }
  public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Integer getMaterialStatus() { return materialStatus; }
  public void setMaterialStatus(Integer materialStatus) { this.materialStatus = materialStatus; }
  public Integer getRssbStatus() { return rssbStatus; }
  public void setRssbStatus(Integer rssbStatus) { this.rssbStatus = rssbStatus; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
