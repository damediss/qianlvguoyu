package com.guoyu.platform.exam.dto;

import com.guoyu.platform.common.api.PageQuery;

public class ExamCourseQueryReq extends PageQuery {

  private String title;

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
}