package com.guoyu.platform.trainingdepartment.dto;

import java.math.BigDecimal;

public class TrainSubsidyUpdateReq {
  private String regionCode;
  private String regionName;
  private String trainMode;
  private Integer policyYear;
  private Long profileId;
  private String policyTemplate;
  private BigDecimal subsidyAmount;
  private Integer studentCount;
  private String remark;
  private Integer sortNumber;

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
  public String getPolicyTemplate() { return policyTemplate; }
  public void setPolicyTemplate(String policyTemplate) { this.policyTemplate = policyTemplate; }
  public BigDecimal getSubsidyAmount() { return subsidyAmount; }
  public void setSubsidyAmount(BigDecimal subsidyAmount) { this.subsidyAmount = subsidyAmount; }
  public Integer getStudentCount() { return studentCount; }
  public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
