package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 培训总结创建请求
 */
public class TrainSummaryCreateReq {
  /** 项目ID */
  @NotNull private Long projectId;
  private BigDecimal certificationRate;
  private BigDecimal subsidySuccessRate;
  private String achievements;
  private String lessonsLearned;
  private String improvementSuggestions;
  /** 内容 */
  private String content;
  private String filePath;
  private Integer approvalStatus;
  private Long approverId;
  private String approverName;
  /** 审批时间 */
  private String approveTime;
  private String rejectReason;
  /** 状态 */
  private Integer status;
  /** 排序号 */
  private Integer sortNumber;

  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public BigDecimal getCertificationRate() { return certificationRate; }
  public void setCertificationRate(BigDecimal certificationRate) { this.certificationRate = certificationRate; }
  public BigDecimal getSubsidySuccessRate() { return subsidySuccessRate; }
  public void setSubsidySuccessRate(BigDecimal subsidySuccessRate) { this.subsidySuccessRate = subsidySuccessRate; }
  public String getAchievements() { return achievements; }
  public void setAchievements(String achievements) { this.achievements = achievements; }
  public String getLessonsLearned() { return lessonsLearned; }
  public void setLessonsLearned(String lessonsLearned) { this.lessonsLearned = lessonsLearned; }
  public String getImprovementSuggestions() { return improvementSuggestions; }
  public void setImprovementSuggestions(String improvementSuggestions) { this.improvementSuggestions = improvementSuggestions; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public Integer getApprovalStatus() { return approvalStatus; }
  public void setApprovalStatus(Integer approvalStatus) { this.approvalStatus = approvalStatus; }
  public Long getApproverId() { return approverId; }
  public void setApproverId(Long approverId) { this.approverId = approverId; }
  public String getApproverName() { return approverName; }
  public void setApproverName(String approverName) { this.approverName = approverName; }
  public String getApproveTime() { return approveTime; }
  public void setApproveTime(String approveTime) { this.approveTime = approveTime; }
  public String getRejectReason() { return rejectReason; }
  public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
