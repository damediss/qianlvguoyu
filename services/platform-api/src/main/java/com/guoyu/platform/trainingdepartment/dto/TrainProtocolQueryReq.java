package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/**
 * 培训协议查询请求
 */
public class TrainProtocolQueryReq extends PageQuery {
  /** 项目ID */
  private Long projectId;
  /** 类型 */
  private String type;
  /** 状态 */
  private Integer status;
  /** 搜索关键词 */
  private String keyword;

  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
