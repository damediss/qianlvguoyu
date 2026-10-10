package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_shift_assign_task")
public class TrainShiftAssignTaskEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long tenantId;
  private Long planId;
  private Long trainInfoId;
  private Integer year;
  private Integer month;
  private Long sourceShiftFileId;
  private String adapterType;
  private String status;
  private Integer version;
  private String warningJson;
  private String configJson;
  private String unassignedJson;
  private String theoryDaysJson;
  private String practiceDaysJson;
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
  public Integer getYear() { return year; }
  public void setYear(Integer year) { this.year = year; }
  public Integer getMonth() { return month; }
  public void setMonth(Integer month) { this.month = month; }
  public Long getSourceShiftFileId() { return sourceShiftFileId; }
  public void setSourceShiftFileId(Long sourceShiftFileId) { this.sourceShiftFileId = sourceShiftFileId; }
  public String getAdapterType() { return adapterType; }
  public void setAdapterType(String adapterType) { this.adapterType = adapterType; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
  public String getWarningJson() { return warningJson; }
  public void setWarningJson(String warningJson) { this.warningJson = warningJson; }
  public String getConfigJson() { return configJson; }
  public void setConfigJson(String configJson) { this.configJson = configJson; }
  public String getUnassignedJson() { return unassignedJson; }
  public void setUnassignedJson(String unassignedJson) { this.unassignedJson = unassignedJson; }
  public String getTheoryDaysJson() { return theoryDaysJson; }
  public void setTheoryDaysJson(String theoryDaysJson) { this.theoryDaysJson = theoryDaysJson; }
  public String getPracticeDaysJson() { return practiceDaysJson; }
  public void setPracticeDaysJson(String practiceDaysJson) { this.practiceDaysJson = practiceDaysJson; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
