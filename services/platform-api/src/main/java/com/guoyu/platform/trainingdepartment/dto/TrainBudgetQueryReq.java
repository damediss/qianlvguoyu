package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/**
 * 培训预算查询请求
 */
public class TrainBudgetQueryReq extends PageQuery {
  /** 培训ID */
  private Long trainId;
  /** 组织ID */
  private Long orgId;
  /** 状态 */
  private Integer status;
  /** 搜索关键词 */
  private String keyword;

  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
