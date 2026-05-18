package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_need")
public class TrainNeedEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long orgId;
  private String orgName;
  private String trainObject;
  private String position;
  private Integer num;
  private String workType;
  private String level;
  private String target;
  private String method;
  private String address;
  private String trainDate;
  private String budget;
  private String teacherRequire;
  private Integer needExam;
  private Integer needMatch;
  private Integer needPaper;
  private Long userId;
  private Integer sortNumber;
  private Long tenantId;
  private LocalDateTime createTime;
  private String jsonData;
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
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
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
