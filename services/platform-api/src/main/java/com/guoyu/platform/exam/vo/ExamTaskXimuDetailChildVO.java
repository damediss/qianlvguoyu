package com.guoyu.platform.exam.vo;

import java.io.Serializable;

public class ExamTaskXimuDetailChildVO implements Serializable {

  private String id;
  private String snoItem;
  private String snoTitle;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getSnoItem() { return snoItem; }
  public void setSnoItem(String snoItem) { this.snoItem = snoItem; }
  public String getSnoTitle() { return snoTitle; }
  public void setSnoTitle(String snoTitle) { this.snoTitle = snoTitle; }
}
