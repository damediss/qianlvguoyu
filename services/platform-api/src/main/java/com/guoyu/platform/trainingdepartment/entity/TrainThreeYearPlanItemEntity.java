package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_three_year_plan_item")
public class TrainThreeYearPlanItemEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long planId;
  private Long tenantId;
  private String personName;
  private String idCard;
  private String companyName;
  private String jobName;
  private String levelName;
  private String trainMode;
  private Integer planYear;
  private Integer trainYear;
  private Integer certYear;
  private String eligibleDate;
  private Integer rowIndex;
  private String source;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getPlanId() { return planId; }
  public void setPlanId(Long planId) { this.planId = planId; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public String getPersonName() { return personName; }
  public void setPersonName(String personName) { this.personName = personName; }
  public String getIdCard() { return idCard; }
  public void setIdCard(String idCard) { this.idCard = idCard; }
  public String getCompanyName() { return companyName; }
  public void setCompanyName(String companyName) { this.companyName = companyName; }
  public String getJobName() { return jobName; }
  public void setJobName(String jobName) { this.jobName = jobName; }
  public String getLevelName() { return levelName; }
  public void setLevelName(String levelName) { this.levelName = levelName; }
  public String getTrainMode() { return trainMode; }
  public void setTrainMode(String trainMode) { this.trainMode = trainMode; }
  public Integer getPlanYear() { return planYear; }
  public void setPlanYear(Integer planYear) { this.planYear = planYear; }
  public Integer getTrainYear() { return trainYear; }
  public void setTrainYear(Integer trainYear) { this.trainYear = trainYear; }
  public Integer getCertYear() { return certYear; }
  public void setCertYear(Integer certYear) { this.certYear = certYear; }
  public String getEligibleDate() { return eligibleDate; }
  public void setEligibleDate(String eligibleDate) { this.eligibleDate = eligibleDate; }
  public Integer getRowIndex() { return rowIndex; }
  public void setRowIndex(Integer rowIndex) { this.rowIndex = rowIndex; }
  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
