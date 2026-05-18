package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

public class TrainNeedUpdateReq {
  private Long orgId;
  @Size(max = 255) private String orgName;
  @Size(max = 255) private String trainObject;
  @Size(max = 255) private String position;
  private Integer num;
  @Size(max = 255) private String workType;
  @Size(max = 255) private String level;
  @Size(max = 255) private String target;
  @Size(max = 255) private String method;
  @Size(max = 255) private String address;
  @Size(max = 255) private String trainDate;
  @Size(max = 255) private String budget;
  private String teacherRequire;
  private Integer needExam;
  private Integer needMatch;
  private Integer needPaper;
  private Integer sortNumber;
  private String jsonData;

  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public String getTrainObject() { return trainObject; }
  public void setTrainObject(String trainObject) { this.trainObject = trainObject; }
  public String getPosition() { return position; }
  public void setPosition(String position) { this.position = position; }
  public Integer getNum() { return num; }
  public void setNum(Integer num) { this.num = num; }
  public String getWorkType() { return workType; }
  public void setWorkType(String workType) { this.workType = workType; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public String getTarget() { return target; }
  public void setTarget(String target) { this.target = target; }
  public String getMethod() { return method; }
  public void setMethod(String method) { this.method = method; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getTrainDate() { return trainDate; }
  public void setTrainDate(String trainDate) { this.trainDate = trainDate; }
  public String getBudget() { return budget; }
  public void setBudget(String budget) { this.budget = budget; }
  public String getTeacherRequire() { return teacherRequire; }
  public void setTeacherRequire(String teacherRequire) { this.teacherRequire = teacherRequire; }
  public Integer getNeedExam() { return needExam; }
  public void setNeedExam(Integer needExam) { this.needExam = needExam; }
  public Integer getNeedMatch() { return needMatch; }
  public void setNeedMatch(Integer needMatch) { this.needMatch = needMatch; }
  public Integer getNeedPaper() { return needPaper; }
  public void setNeedPaper(Integer needPaper) { this.needPaper = needPaper; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public String getJsonData() { return jsonData; }
  public void setJsonData(String jsonData) { this.jsonData = jsonData; }
}
