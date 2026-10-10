package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培训总结实体
 */
@TableName("train_summary")
public class TrainSummaryEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 项目ID */
  private Long projectId;
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
  /** 租户ID */
  private Long tenantId;
  /** 用户ID */
  private Long userId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 更新时间 */
  private LocalDateTime updateTime;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
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
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
