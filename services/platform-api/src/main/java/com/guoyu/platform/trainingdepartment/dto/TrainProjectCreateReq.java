package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/**
 * 培训项目创建请求
 */
public class TrainProjectCreateReq {
  @Size(max = 255) private String projectName;
  private Long companyId;
  @Size(max = 255) private String companyName;
  private Long managerId;
  @Size(max = 100) private String managerName;
  @Size(max = 100) private String planStart;
  @Size(max = 100) private String planEnd;
  @Size(max = 100) private String currentStage;
  /** 进度 */
  private Integer progress;
  /** 状态 */
  private Integer status;
  /** 描述 */
  private String description;
  /** 排序号 */
  private Integer sortNumber;

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
}
