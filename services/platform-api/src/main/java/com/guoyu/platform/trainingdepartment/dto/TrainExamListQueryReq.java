package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

/** 考试计划查询请求 */
public class TrainExamListQueryReq extends PageQuery {

  private Long trainId;
  private Long orgId;
  private Integer type;
  private Integer source;
  private String keyword;

  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public Integer getSource() { return source; }
  public void setSource(Integer source) { this.source = source; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
