package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TrainSubsidyCreateReq {
  /** 首期强制班级 */
  @NotNull(message = "请选择班级")
  private Long classId;
  private Long projectId;
  @NotBlank(message = "请选择培训模式")
  private String trainMode;
  private String regionCode;
  private String regionName;
  private Integer policyYear;
  private Long profileId;
  private String policyTemplate;
  private BigDecimal subsidyAmount;
  private Integer studentCount;
  private String remark;

  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public String getTrainMode() { return trainMode; }
  public void setTrainMode(String trainMode) { this.trainMode = trainMode; }
  public String getRegionCode() { return regionCode; }
  public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
  public String getRegionName() { return regionName; }
  public void setRegionName(String regionName) { this.regionName = regionName; }
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
}
