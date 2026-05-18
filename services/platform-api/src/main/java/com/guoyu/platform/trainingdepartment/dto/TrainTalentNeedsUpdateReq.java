package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

public class TrainTalentNeedsUpdateReq {
  @Size(max = 255) private String title;
  private Long orgId;
  @Size(max = 255) private String orgName;
  @Size(max = 255) private String job;
  @Size(max = 255) private String workType;
  @Size(max = 255) private String major;
  @Size(max = 255) private String edu;
  private Integer gender;
  private Integer num;
  @Size(max = 255) private String address;
  @Size(max = 255) private String workYear;
  @Size(max = 255) private String inPositionDate;
  private String paperRequire;
  private Integer sortNumber;
  private String jsonData;
  private String duty;
  private String schoolNeed;

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getOrgName() { return orgName; }
  public void setOrgName(String orgName) { this.orgName = orgName; }
  public String getJob() { return job; }
  public void setJob(String job) { this.job = job; }
  public String getWorkType() { return workType; }
  public void setWorkType(String workType) { this.workType = workType; }
  public String getMajor() { return major; }
  public void setMajor(String major) { this.major = major; }
  public String getEdu() { return edu; }
  public void setEdu(String edu) { this.edu = edu; }
  public Integer getGender() { return gender; }
  public void setGender(Integer gender) { this.gender = gender; }
  public Integer getNum() { return num; }
  public void setNum(Integer num) { this.num = num; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getWorkYear() { return workYear; }
  public void setWorkYear(String workYear) { this.workYear = workYear; }
  public String getInPositionDate() { return inPositionDate; }
  public void setInPositionDate(String inPositionDate) { this.inPositionDate = inPositionDate; }
  public String getPaperRequire() { return paperRequire; }
  public void setPaperRequire(String paperRequire) { this.paperRequire = paperRequire; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public String getJsonData() { return jsonData; }
  public void setJsonData(String jsonData) { this.jsonData = jsonData; }
  public String getDuty() { return duty; }
  public void setDuty(String duty) { this.duty = duty; }
  public String getSchoolNeed() { return schoolNeed; }
  public void setSchoolNeed(String schoolNeed) { this.schoolNeed = schoolNeed; }
}
