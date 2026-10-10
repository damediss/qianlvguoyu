package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("train_subsidy")
public class TrainSubsidyEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long projectId;
  private Long classId;
  private Long trainInfoId;
  private String policyTemplate;
  private BigDecimal subsidyAmount;
  private Integer studentCount;
  private String materialPackagePath;
  private Integer materialStatus;
  private Integer rssbStatus;
  private String rssbSubmitTime;
  private String rssbReceiptNo;
  private String rssbRejectReason;
  private BigDecimal grantedAmount;
  private String grantTime;
  private String remark;
  private Integer status;
  private String phase;
  private String regionCode;
  private String regionName;
  private String trainMode;
  private Integer policyYear;
  private Long profileId;
  private String profileVersion;
  private Integer profileLocked;
  private String checklistJson;
  private String evidenceIndexPath;
  private Integer sortNumber;
  private Long tenantId;
  private Long userId;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }
  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
  public String getPolicyTemplate() { return policyTemplate; }
  public void setPolicyTemplate(String policyTemplate) { this.policyTemplate = policyTemplate; }
  public BigDecimal getSubsidyAmount() { return subsidyAmount; }
  public void setSubsidyAmount(BigDecimal subsidyAmount) { this.subsidyAmount = subsidyAmount; }
  public Integer getStudentCount() { return studentCount; }
  public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }
  public String getMaterialPackagePath() { return materialPackagePath; }
  public void setMaterialPackagePath(String materialPackagePath) { this.materialPackagePath = materialPackagePath; }
  public Integer getMaterialStatus() { return materialStatus; }
  public void setMaterialStatus(Integer materialStatus) { this.materialStatus = materialStatus; }
  public Integer getRssbStatus() { return rssbStatus; }
  public void setRssbStatus(Integer rssbStatus) { this.rssbStatus = rssbStatus; }
  public String getRssbSubmitTime() { return rssbSubmitTime; }
  public void setRssbSubmitTime(String rssbSubmitTime) { this.rssbSubmitTime = rssbSubmitTime; }
  public String getRssbReceiptNo() { return rssbReceiptNo; }
  public void setRssbReceiptNo(String rssbReceiptNo) { this.rssbReceiptNo = rssbReceiptNo; }
  public String getRssbRejectReason() { return rssbRejectReason; }
  public void setRssbRejectReason(String rssbRejectReason) { this.rssbRejectReason = rssbRejectReason; }
  public BigDecimal getGrantedAmount() { return grantedAmount; }
  public void setGrantedAmount(BigDecimal grantedAmount) { this.grantedAmount = grantedAmount; }
  public String getGrantTime() { return grantTime; }
  public void setGrantTime(String grantTime) { this.grantTime = grantTime; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getPhase() { return phase; }
  public void setPhase(String phase) { this.phase = phase; }
  public String getRegionCode() { return regionCode; }
  public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
  public String getRegionName() { return regionName; }
  public void setRegionName(String regionName) { this.regionName = regionName; }
  public String getTrainMode() { return trainMode; }
  public void setTrainMode(String trainMode) { this.trainMode = trainMode; }
  public Integer getPolicyYear() { return policyYear; }
  public void setPolicyYear(Integer policyYear) { this.policyYear = policyYear; }
  public Long getProfileId() { return profileId; }
  public void setProfileId(Long profileId) { this.profileId = profileId; }
  public String getProfileVersion() { return profileVersion; }
  public void setProfileVersion(String profileVersion) { this.profileVersion = profileVersion; }
  public Integer getProfileLocked() { return profileLocked; }
  public void setProfileLocked(Integer profileLocked) { this.profileLocked = profileLocked; }
  public String getChecklistJson() { return checklistJson; }
  public void setChecklistJson(String checklistJson) { this.checklistJson = checklistJson; }
  public String getEvidenceIndexPath() { return evidenceIndexPath; }
  public void setEvidenceIndexPath(String evidenceIndexPath) { this.evidenceIndexPath = evidenceIndexPath; }
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
