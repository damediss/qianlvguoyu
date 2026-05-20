package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 培训需求实体
 */
@TableName("train_need")
public class TrainNeedEntity {
  /** 主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;
  /** 组织ID */
  private Long orgId;
  /** 组织名称 */
  private String orgName;
  /** 培训对象 */
  private String trainObject;
  /** 岗位 */
  private String position;
  /** 人数 */
  private Integer num;
  /** 工种 */
  private String workType;
  /** 等级 */
  private String level;
  /** 培训目标 */
  private String target;
  /** 培训方式 */
  private String method;
  /** 培训地点 */
  private String address;
  /** 培训日期 */
  private String trainDate;
  /** 预算 */
  private String budget;
  /** 师资要求 */
  private String teacherRequire;
  /** 是否需要考试 0-否 1-是 */
  private Integer needExam;
  /** 是否需要匹配 0-否 1-是 */
  private Integer needMatch;
  /** 是否需要试卷 0-否 1-是 */
  private Integer needPaper;
  /** 用户ID */
  private Long userId;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 扩展JSON数据 */
  private String jsonData;
  /** 逻辑删除标记 0-未删除 1-已删除 */
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
