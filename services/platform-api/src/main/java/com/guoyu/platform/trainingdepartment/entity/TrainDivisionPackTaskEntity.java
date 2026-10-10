package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_division_pack_task")
public class TrainDivisionPackTaskEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long tenantId;
  private Long planId;
  private Long trainInfoId;
  private Long companyId;
  private String strategy;
  private Integer classMax;
  private Integer year;
  private Integer month;
  private String classNamePrefix;
  private String status;
  private Integer classCount;
  private Integer assignedCount;
  private Integer unassignedCount;
  private String classesJson;
  private String unassignedJson;
  private String warningJson;
  private String rejectReason;
  private Long submitUserId;
  private String submitUsername;
  private Long confirmUserId;
  private String confirmUsername;
  private LocalDateTime confirmTime;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getPlanId() { return planId; }
  public void setPlanId(Long planId) { this.planId = planId; }
  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
  public String getStrategy() { return strategy; }
  public void setStrategy(String strategy) { this.strategy = strategy; }
  public Integer getClassMax() { return classMax; }
  public void setClassMax(Integer classMax) { this.classMax = classMax; }
  public Integer getYear() { return year; }
  public void setYear(Integer year) { this.year = year; }
  public Integer getMonth() { return month; }
  public void setMonth(Integer month) { this.month = month; }
  public String getClassNamePrefix() { return classNamePrefix; }
  public void setClassNamePrefix(String classNamePrefix) { this.classNamePrefix = classNamePrefix; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getClassCount() { return classCount; }
  public void setClassCount(Integer classCount) { this.classCount = classCount; }
  public Integer getAssignedCount() { return assignedCount; }
  public void setAssignedCount(Integer assignedCount) { this.assignedCount = assignedCount; }
  public Integer getUnassignedCount() { return unassignedCount; }
  public void setUnassignedCount(Integer unassignedCount) { this.unassignedCount = unassignedCount; }
  public String getClassesJson() { return classesJson; }
  public void setClassesJson(String classesJson) { this.classesJson = classesJson; }
  public String getUnassignedJson() { return unassignedJson; }
  public void setUnassignedJson(String unassignedJson) { this.unassignedJson = unassignedJson; }
  public String getWarningJson() { return warningJson; }
  public void setWarningJson(String warningJson) { this.warningJson = warningJson; }
  public String getRejectReason() { return rejectReason; }
  public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
  public Long getSubmitUserId() { return submitUserId; }
  public void setSubmitUserId(Long submitUserId) { this.submitUserId = submitUserId; }
  public String getSubmitUsername() { return submitUsername; }
  public void setSubmitUsername(String submitUsername) { this.submitUsername = submitUsername; }
  public Long getConfirmUserId() { return confirmUserId; }
  public void setConfirmUserId(Long confirmUserId) { this.confirmUserId = confirmUserId; }
  public String getConfirmUsername() { return confirmUsername; }
  public void setConfirmUsername(String confirmUsername) { this.confirmUsername = confirmUsername; }
  public LocalDateTime getConfirmTime() { return confirmTime; }
  public void setConfirmTime(LocalDateTime confirmTime) { this.confirmTime = confirmTime; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
