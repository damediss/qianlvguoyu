package com.guoyu.platform.exam.vo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class ExamTaskXimuVO implements Serializable {

  private Integer id;
  private Integer workTypeId;
  private Integer level;
  private Integer types;
  private Integer nums;
  private Integer status;
  private Integer expertStart;
  private Integer expertEnd;
  private String filePath;
  private String fileUrl;
  private String fileType;
  private LocalDateTime addtime;

  private String workTypeCn;
  private String levelCn;
  private String typesCn;
  private String statusCn;
  private String expertStartName;
  private String expertEndName;
  private String addtimeCn;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Integer workTypeId) { this.workTypeId = workTypeId; }
  public Integer getLevel() { return level; }
  public void setLevel(Integer level) { this.level = level; }
  public Integer getTypes() { return types; }
  public void setTypes(Integer types) { this.types = types; }
  public Integer getNums() { return nums; }
  public void setNums(Integer nums) { this.nums = nums; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getExpertStart() { return expertStart; }
  public void setExpertStart(Integer expertStart) { this.expertStart = expertStart; }
  public Integer getExpertEnd() { return expertEnd; }
  public void setExpertEnd(Integer expertEnd) { this.expertEnd = expertEnd; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public String getFileUrl() { return fileUrl; }
  public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
  public String getFileType() { return fileType; }
  public void setFileType(String fileType) { this.fileType = fileType; }
  public LocalDateTime getAddtime() { return addtime; }
  public void setAddtime(LocalDateTime addtime) { this.addtime = addtime; }
  public String getWorkTypeCn() { return workTypeCn; }
  public void setWorkTypeCn(String workTypeCn) { this.workTypeCn = workTypeCn; }
  public String getLevelCn() { return levelCn; }
  public void setLevelCn(String levelCn) { this.levelCn = levelCn; }
  public String getTypesCn() { return typesCn; }
  public void setTypesCn(String typesCn) { this.typesCn = typesCn; }
  public String getStatusCn() { return statusCn; }
  public void setStatusCn(String statusCn) { this.statusCn = statusCn; }
  public String getExpertStartName() { return expertStartName; }
  public void setExpertStartName(String expertStartName) { this.expertStartName = expertStartName; }
  public String getExpertEndName() { return expertEndName; }
  public void setExpertEndName(String expertEndName) { this.expertEndName = expertEndName; }
  public String getAddtimeCn() { return addtimeCn; }
  public void setAddtimeCn(String addtimeCn) { this.addtimeCn = addtimeCn; }
}
