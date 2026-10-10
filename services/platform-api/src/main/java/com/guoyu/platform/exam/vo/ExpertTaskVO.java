package com.guoyu.platform.exam.vo;

import java.util.List;
import java.util.Map;

public class ExpertTaskVO {

  private Integer id;
  private Integer taskId;
  private String tag;
  private String tagCn;
  private String title;
  private Integer nums;
  private String addtimeCn;
  private String endtimeCn;
  private Long endtime;
  private String status;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public String getTag() { return tag; }
  public void setTag(String tag) { this.tag = tag; }
  public String getTagCn() { return tagCn; }
  public void setTagCn(String tagCn) { this.tagCn = tagCn; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getNums() { return nums; }
  public void setNums(Integer nums) { this.nums = nums; }
  public String getAddtimeCn() { return addtimeCn; }
  public void setAddtimeCn(String addtimeCn) { this.addtimeCn = addtimeCn; }
  public String getEndtimeCn() { return endtimeCn; }
  public void setEndtimeCn(String endtimeCn) { this.endtimeCn = endtimeCn; }
  public Long getEndtime() { return endtime; }
  public void setEndtime(Long endtime) { this.endtime = endtime; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
