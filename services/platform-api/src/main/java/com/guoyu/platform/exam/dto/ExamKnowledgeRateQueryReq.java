package com.guoyu.platform.exam.dto;

import com.guoyu.platform.common.api.PageQuery;

public class ExamKnowledgeRateQueryReq extends PageQuery {

  private Integer courseId;
  private String keyword;

  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
