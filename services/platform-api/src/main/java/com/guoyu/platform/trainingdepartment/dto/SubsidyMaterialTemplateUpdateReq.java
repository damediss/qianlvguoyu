package com.guoyu.platform.trainingdepartment.dto;

/** 更新补贴材料模板元数据 */
public class SubsidyMaterialTemplateUpdateReq {

  private String title;
  private String materialType;
  private String slotCode;
  private String regionCode;
  private String trainMode;
  private Integer policyYear;
  private String scope;
  private Long classId;
  private String status;
  private String remark;

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getMaterialType() { return materialType; }
  public void setMaterialType(String materialType) { this.materialType = materialType; }
  public String getSlotCode() { return slotCode; }
  public void setSlotCode(String slotCode) { this.slotCode = slotCode; }
  public String getRegionCode() { return regionCode; }
  public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
  public String getTrainMode() { return trainMode; }
  public void setTrainMode(String trainMode) { this.trainMode = trainMode; }
  public Integer getPolicyYear() { return policyYear; }
  public void setPolicyYear(Integer policyYear) { this.policyYear = policyYear; }
  public String getScope() { return scope; }
  public void setScope(String scope) { this.scope = scope; }
  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
}
