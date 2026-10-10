package com.guoyu.platform.exam.dto;

import com.guoyu.platform.common.api.PageQuery;

public class ExamTaskExamQueryReq extends PageQuery {
  private String keyword;
  private Integer status;
  private Integer workTypeId;
  private Integer level;

  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public Integer getLevel() { return level; }
  public void setLevel(Integer level) { this.level = level; }
}
