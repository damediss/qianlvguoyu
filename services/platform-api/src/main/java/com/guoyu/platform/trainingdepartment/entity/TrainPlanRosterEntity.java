package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_plan_roster")
public class TrainPlanRosterEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long tenantId;
  private Long planId;
  private Long trainInfoId;
  private String source;
  private String personName;
  private String idCard;
  private String phone;
  private String jobName;
  private String levelName;
  private String workshop;
  private String shiftTeam;
  private Integer planYear;
  private String trainMode;
  /** 0待分 1已分班 2排除 */
  private Integer assignStatus;
  private Long trainClassId;
  private String rowJson;
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
  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }
  public String getPersonName() { return personName; }
  public void setPersonName(String personName) { this.personName = personName; }
  public String getIdCard() { return idCard; }
  public void setIdCard(String idCard) { this.idCard = idCard; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getJobName() { return jobName; }
  public void setJobName(String jobName) { this.jobName = jobName; }
  public String getLevelName() { return levelName; }
  public void setLevelName(String levelName) { this.levelName = levelName; }
  public String getWorkshop() { return workshop; }
  public void setWorkshop(String workshop) { this.workshop = workshop; }
  public String getShiftTeam() { return shiftTeam; }
  public void setShiftTeam(String shiftTeam) { this.shiftTeam = shiftTeam; }
  public Integer getPlanYear() { return planYear; }
  public void setPlanYear(Integer planYear) { this.planYear = planYear; }
  public String getTrainMode() { return trainMode; }
  public void setTrainMode(String trainMode) { this.trainMode = trainMode; }
  public Integer getAssignStatus() { return assignStatus; }
  public void setAssignStatus(Integer assignStatus) { this.assignStatus = assignStatus; }
  public Long getTrainClassId() { return trainClassId; }
  public void setTrainClassId(Long trainClassId) { this.trainClassId = trainClassId; }
  public String getRowJson() { return rowJson; }
  public void setRowJson(String rowJson) { this.rowJson = rowJson; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
