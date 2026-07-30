package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 培训项目实体
 */
@TableName("train_project")
public class TrainProjectEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  private String projectName;
  private Long companyId;
  private String companyName;
  private Long managerId;
  private String managerName;
  private String planStart;
  private String planEnd;
  private String currentStage;
  /** 进度 */
  private Integer progress;
  /** 状态 */
  private Integer status;
  /** 描述 */
  private String description;
  /** 排序号 */
  private Integer sortNumber;

  /**  租户ID */
  @TableField(fill = FieldFill.INSERT)
  private Long tenantId;

  @TableField(fill = FieldFill.INSERT)
  private Long createdBy;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private Long updatedBy;

  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updatedAt;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getProjectName() { return projectName; }
  public void setProjectName(String projectName) { this.projectName = projectName; }
  public Long getCompanyId() { return companyId; }
  public void setCompanyId(Long companyId) { this.companyId = companyId; }
  public String getCompanyName() { return companyName; }
  public void setCompanyName(String companyName) { this.companyName = companyName; }
  public Long getManagerId() { return managerId; }
  public void setManagerId(Long managerId) { this.managerId = managerId; }
  public String getManagerName() { return managerName; }
  public void setManagerName(String managerName) { this.managerName = managerName; }
  public String getPlanStart() { return planStart; }
  public void setPlanStart(String planStart) { this.planStart = planStart; }
  public String getPlanEnd() { return planEnd; }
  public void setPlanEnd(String planEnd) { this.planEnd = planEnd; }
  public String getCurrentStage() { return currentStage; }
  public void setCurrentStage(String currentStage) { this.currentStage = currentStage; }
  public Integer getProgress() { return progress; }
  public void setProgress(Integer progress) { this.progress = progress; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
