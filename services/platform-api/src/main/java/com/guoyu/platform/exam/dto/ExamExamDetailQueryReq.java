package com.guoyu.platform.exam.dto;

import com.guoyu.platform.common.api.PageQuery;

public class ExamExamDetailQueryReq extends PageQuery {

  private Integer courseId;
  private Integer knowledgeId;
  private String type;
  private Integer source;
  private Integer difficulty;
  private Integer status;
  private String keyword;

  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public Integer getKnowledgeId() { return knowledgeId; }
  public void setKnowledgeId(Integer knowledgeId) { this.knowledgeId = knowledgeId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Integer getSource() { return source; }
  public void setSource(Integer source) { this.source = source; }
  public Integer getDifficulty() { return difficulty; }
  public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getKeyword() { return keyword; }
  public void setKeyword(String keyword) { this.keyword = keyword; }
}
