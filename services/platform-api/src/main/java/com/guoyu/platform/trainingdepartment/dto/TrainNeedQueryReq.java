package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/**
 * 培训需求查询请求
 */
public class TrainNeedQueryReq extends PageQuery {
  /** 组织ID */
  private Long orgId;
  /** 搜索关键词 */
  private String keyword;

  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
