package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/**
 * 培训项目表单查询请求
 */
public class TrainProjectTableQueryReq extends PageQuery {
  /** 项目ID */
  private Long projectId;
  private Integer tableType;
  /** 状态 */
  private Integer status;
  /** 搜索关键词 */
  private String keyword;

  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Integer getTableType() { return tableType; }
  public void setTableType(Integer tableType) { this.tableType = tableType; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
