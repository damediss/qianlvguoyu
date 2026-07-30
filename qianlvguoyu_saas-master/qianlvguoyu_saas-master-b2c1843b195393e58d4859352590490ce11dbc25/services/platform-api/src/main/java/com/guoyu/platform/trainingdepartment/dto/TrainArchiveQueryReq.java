package com.guoyu.platform.trainingdepartment.dto;

/**
 * 培训档案查询请求
 */
public class TrainArchiveQueryReq {
  private Integer page = 1;
  private Integer pageSize = 20;
  /** 项目ID */
  private Long projectId;
  private String archiveType;
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
  public String getArchiveType() { return archiveType; }
  public void setArchiveType(String archiveType) { this.archiveType = archiveType; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
