package com.guoyu.platform.exam.dto;

import com.guoyu.platform.common.api.PageQuery;

public class ExamCourseQueryReq extends PageQuery {

  private Integer workTypeId;
  private String title;

  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
}