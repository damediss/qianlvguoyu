package com.guoyu.platform.exam.knowledgefix.dto;

/** exam_knowledge_structure 节点行（课程树快照输入） */
public class KnowledgeNodeRow {
  private Integer id;
  private Integer courseId;
  private Integer parentId;
  private String title;
  private Integer level;
  private String important;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getCourseId() { return courseId; }
  public void setCourseId(Integer courseId) { this.courseId = courseId; }
  public Integer getParentId() { return parentId; }
  public void setParentId(Integer parentId) { this.parentId = parentId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getLevel() { return level; }
  public void setLevel(Integer level) { this.level = level; }
  public String getImportant() { return important; }
  public void setImportant(String important) { this.important = important; }
}
