package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.common.api.PageQuery;

public class TrainTalentNeedsQueryReq extends PageQuery {
  private Long orgId;
  private String keyword;
  private Integer gender;
  private String edu;

  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getGender() { return gender; }
  public void setGender(Integer gender) { this.gender = gender; }
  public String getEdu() { return edu; }
  public void setEdu(String edu) { this.edu = edu; }
}
