package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 培训补贴创建请求
 */
public class TrainSubsidyCreateReq {
  /** 项目ID */
  @NotNull private Long projectId;
  private String policyTemplate;
  /** 补贴金额 */
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
  /** 备注 */
  private String remark;
  /** 状态 */
  private Integer status;
  /** 排序号 */
  private Integer sortNumber;

  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
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
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
