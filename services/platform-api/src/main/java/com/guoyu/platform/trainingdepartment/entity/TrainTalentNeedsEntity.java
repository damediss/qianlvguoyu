package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_talent_needs")
public class TrainTalentNeedsEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String title;
  private Long orgId;
  private String orgName;
  private String job;
  private String workType;
  private String major;
  private String edu;
  private Integer gender;
  private Integer num;
  private String address;
  private String workYear;
  private String inPositionDate;
  private String paperRequire;
  private Long userId;
  private Integer sortNumber;
  private Long tenantId;
  private LocalDateTime createTime;
  private String jsonData;
  private String duty;
  private String schoolNeed;
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
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
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public String getJsonData() { return jsonData; }
  public void setJsonData(String jsonData) { this.jsonData = jsonData; }
  public String getDuty() { return duty; }
  public void setDuty(String duty) { this.duty = duty; }
  public String getSchoolNeed() { return schoolNeed; }
  public void setSchoolNeed(String schoolNeed) { this.schoolNeed = schoolNeed; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
