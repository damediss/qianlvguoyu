package com.guoyu.platform.exam.vo;

import java.io.Serializable;
import java.util.List;

public class ExamTaskXimuDetailVO implements Serializable {

  private Integer id;
  private Integer taskId;
  private String sno;
  private String sno0;
  private String sno1;
  private String title;
  private String snoItem;
  private String snoTitle;
  private String rowColumn;
  private List<ExamTaskXimuDetailChildVO> children;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public String getSno() { return sno; }
  public void setSno(String sno) { this.sno = sno; }
  public String getSno0() { return sno0; }
  public void setSno0(String sno0) { this.sno0 = sno0; }
  public String getSno1() { return sno1; }
  public void setSno1(String sno1) { this.sno1 = sno1; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getSnoItem() { return snoItem; }
  public void setSnoItem(String snoItem) { this.snoItem = snoItem; }
  public String getSnoTitle() { return snoTitle; }
  public void setSnoTitle(String snoTitle) { this.snoTitle = snoTitle; }
  public String getRowColumn() { return rowColumn; }
  public void setRowColumn(String rowColumn) { this.rowColumn = rowColumn; }
  public List<ExamTaskXimuDetailChildVO> getChildren() { return children; }
  public void setChildren(List<ExamTaskXimuDetailChildVO> children) { this.children = children; }
}
