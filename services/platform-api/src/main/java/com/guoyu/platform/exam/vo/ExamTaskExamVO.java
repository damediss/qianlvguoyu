package com.guoyu.platform.exam.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ExamTaskExamVO implements Serializable {

  private Integer id;
  private Integer workTypeId;
  private String workTypeCn;
  private Integer level;
  private String levelCn;
  private Integer nums;
  private Integer status;
  private String statusCn;
  private Integer expertStart;
  private String expertStartName;
  private Integer expertEnd;
  private String expertEndName;
  private String filePath;
  private LocalDateTime addtime;
  private String addtimeCn;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public String getWorkTypeCn() { return workTypeCn; }
  public void setWorkTypeCn(String workTypeCn) { this.workTypeCn = workTypeCn; }
  public Integer getLevel() { return level; }
  public void setLevel(Integer level) { this.level = level; }
  public String getLevelCn() { return levelCn; }
  public void setLevelCn(String levelCn) { this.levelCn = levelCn; }
  public Integer getNums() { return nums; }
  public void setNums(Integer nums) { this.nums = nums; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getStatusCn() { return statusCn; }
  public void setStatusCn(String statusCn) { this.statusCn = statusCn; }
  public Integer getExpertStart() { return expertStart; }
  public void setExpertStart(Integer expertStart) { this.expertStart = expertStart; }
  public String getExpertStartName() { return expertStartName; }
  public void setExpertStartName(String expertStartName) { this.expertStartName = expertStartName; }
  public Integer getExpertEnd() { return expertEnd; }
  public void setExpertEnd(Integer expertEnd) { this.expertEnd = expertEnd; }
  public String getExpertEndName() { return expertEndName; }
  public void setExpertEndName(String expertEndName) { this.expertEndName = expertEndName; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public LocalDateTime getAddtime() { return addtime; }
  public void setAddtime(LocalDateTime addtime) { this.addtime = addtime; }
  public String getAddtimeCn() { return addtimeCn; }
  public void setAddtimeCn(String addtimeCn) { this.addtimeCn = addtimeCn; }
}
