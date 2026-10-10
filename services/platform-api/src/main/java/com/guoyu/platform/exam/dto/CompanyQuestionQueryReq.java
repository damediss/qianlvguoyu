package com.guoyu.platform.exam.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompanyQuestionQueryReq extends PageQuery {

  private Long orgId;
  private String type;
  private Integer difficulty;
  private Integer status;
  private String keyword;

  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Integer getDifficulty() { return difficulty; }
  public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
