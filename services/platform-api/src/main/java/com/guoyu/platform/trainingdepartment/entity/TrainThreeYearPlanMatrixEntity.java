package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_three_year_plan_matrix")
public class TrainThreeYearPlanMatrixEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long planId;
  private Long tenantId;
  private String jobName;
  private Integer planYear;
  private String trainMode;
  private String levelName;
  private Integer headcount;
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
  public String getJobName() { return jobName; }
  public void setJobName(String jobName) { this.jobName = jobName; }
  public Integer getPlanYear() { return planYear; }
  public void setPlanYear(Integer planYear) { this.planYear = planYear; }
  public String getTrainMode() { return trainMode; }
  public void setTrainMode(String trainMode) { this.trainMode = trainMode; }
  public String getLevelName() { return levelName; }
  public void setLevelName(String levelName) { this.levelName = levelName; }
  public Integer getHeadcount() { return headcount; }
  public void setHeadcount(Integer headcount) { this.headcount = headcount; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
