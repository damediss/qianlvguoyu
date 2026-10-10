package com.guoyu.platform.exam.dto;

import com.guoyu.platform.common.api.PageQuery;

public class ExamKnowledgeStructureQueryReq extends PageQuery {

  private Integer courseId;
  private Integer type;
  private String title;
  private Integer level;

  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public Integer getType() { return type; }
  public void setType(Integer type) { this.type = type; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getLevel() { return level; }
  public void setLevel(Integer level) { this.level = level; }
}
