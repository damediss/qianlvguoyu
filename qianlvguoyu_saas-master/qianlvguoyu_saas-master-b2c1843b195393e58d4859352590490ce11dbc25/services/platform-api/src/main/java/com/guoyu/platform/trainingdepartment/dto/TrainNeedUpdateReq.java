package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/**
 * 培训需求更新请求
 */
public class TrainNeedUpdateReq {
  /** 组织ID */
  private Long orgId;
  /** 组织名称 */
  @Size(max = 255) private String orgName;
  /** 培训对象 */
  @Size(max = 255) private String trainObject;
  /** 岗位 */
  @Size(max = 255) private String position;
  /** 人数 */
  private Integer num;
  /** 工种 */
  @Size(max = 255) private String workType;
  /** 等级 */
  @Size(max = 255) private String level;
  /** 培训目标 */
  @Size(max = 255) private String target;
  /** 培训方式 */
  @Size(max = 255) private String method;
  /** 培训地点 */
  @Size(max = 255) private String address;
  /** 培训日期 */
  @Size(max = 255) private String trainDate;
  /** 预算 */
  @Size(max = 255) private String budget;
  /** 师资要求 */
  private String teacherRequire;
  /** 是否需要考试 0-否 1-是 */
  private Integer needExam;
  /** 是否需要匹配 0-否 1-是 */
  private Integer needMatch;
  /** 是否需要试卷 0-否 1-是 */
  private Integer needPaper;
  /** 排序号 */
  private Integer sortNumber;
  /** 扩展JSON数据 */
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
