package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_subsidy_pack_profile")
public class TrainSubsidyPackProfileEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String regionCode;
  private String regionName;
  private String trainMode;
  private Integer policyYear;
  private String profileVersion;
  private String title;
  private String status;
  private Integer isDefault;
  private String remark;
  private Long tenantId;
  @TableLogic
  private Integer deleted;
  private Long createdBy;
  private Long updatedBy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getRegionCode() { return regionCode; }
  public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
  public String getRegionName() { return regionName; }
  public void setRegionName(String regionName) { this.regionName = regionName; }
  public String getTrainMode() { return trainMode; }
  public void setTrainMode(String trainMode) { this.trainMode = trainMode; }
  public Integer getPolicyYear() { return policyYear; }
  public void setPolicyYear(Integer policyYear) { this.policyYear = policyYear; }
  public String getProfileVersion() { return profileVersion; }
  public void setProfileVersion(String profileVersion) { this.profileVersion = profileVersion; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getIsDefault() { return isDefault; }
  public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public Long getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
