package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_three_year_plan")
public class TrainThreeYearPlanEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long tenantId;
  private String planNo;
  private String title;
  private Long targetOrgId;
  private Long researchOrgId;
  private Long sourceSignUpId;
  private Long rencaiTaskId;
  private Long rencaiInputFileId;
  private String planYears;
  private Integer status;
  private String analysisStatus;
  private String distributionSnapshot;
  private Long reviewedPlanFileId;
  private Long submitPersonnelFileId;
  private Long submitSchemeFileId;
  private String rejectReason;
  private String rejectHistoryJson;
  private String submitHistoryJson;
  private LocalDateTime companyConfirmTime;
  private Long companyConfirmUserId;
  private String companyConfirmUsername;
  private Long createdBy;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public String getPlanNo() { return planNo; }
  public void setPlanNo(String planNo) { this.planNo = planNo; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Long getTargetOrgId() { return targetOrgId; }
  public void setTargetOrgId(Long targetOrgId) { this.targetOrgId = targetOrgId; }
  public Long getResearchOrgId() { return researchOrgId; }
  public void setResearchOrgId(Long researchOrgId) { this.researchOrgId = researchOrgId; }
  public Long getSourceSignUpId() { return sourceSignUpId; }
  public void setSourceSignUpId(Long sourceSignUpId) { this.sourceSignUpId = sourceSignUpId; }
  public Long getRencaiTaskId() { return rencaiTaskId; }
  public void setRencaiTaskId(Long rencaiTaskId) { this.rencaiTaskId = rencaiTaskId; }
  public Long getRencaiInputFileId() { return rencaiInputFileId; }
  public void setRencaiInputFileId(Long rencaiInputFileId) { this.rencaiInputFileId = rencaiInputFileId; }
  public String getPlanYears() { return planYears; }
  public void setPlanYears(String planYears) { this.planYears = planYears; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getAnalysisStatus() { return analysisStatus; }
  public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }
  public String getDistributionSnapshot() { return distributionSnapshot; }
  public void setDistributionSnapshot(String distributionSnapshot) { this.distributionSnapshot = distributionSnapshot; }
  public Long getReviewedPlanFileId() { return reviewedPlanFileId; }
  public void setReviewedPlanFileId(Long reviewedPlanFileId) { this.reviewedPlanFileId = reviewedPlanFileId; }
  public Long getSubmitPersonnelFileId() { return submitPersonnelFileId; }
  public void setSubmitPersonnelFileId(Long submitPersonnelFileId) { this.submitPersonnelFileId = submitPersonnelFileId; }
  public Long getSubmitSchemeFileId() { return submitSchemeFileId; }
  public void setSubmitSchemeFileId(Long submitSchemeFileId) { this.submitSchemeFileId = submitSchemeFileId; }
  public String getRejectReason() { return rejectReason; }
  public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
  public String getRejectHistoryJson() { return rejectHistoryJson; }
  public void setRejectHistoryJson(String rejectHistoryJson) { this.rejectHistoryJson = rejectHistoryJson; }
  public String getSubmitHistoryJson() { return submitHistoryJson; }
  public void setSubmitHistoryJson(String submitHistoryJson) { this.submitHistoryJson = submitHistoryJson; }
  public LocalDateTime getCompanyConfirmTime() { return companyConfirmTime; }
  public void setCompanyConfirmTime(LocalDateTime companyConfirmTime) { this.companyConfirmTime = companyConfirmTime; }
  public Long getCompanyConfirmUserId() { return companyConfirmUserId; }
  public void setCompanyConfirmUserId(Long companyConfirmUserId) { this.companyConfirmUserId = companyConfirmUserId; }
  public String getCompanyConfirmUsername() { return companyConfirmUsername; }
  public void setCompanyConfirmUsername(String companyConfirmUsername) { this.companyConfirmUsername = companyConfirmUsername; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
