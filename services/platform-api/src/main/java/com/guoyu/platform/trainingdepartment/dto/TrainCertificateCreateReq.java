package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 培训证书创建请求
 */
public class TrainCertificateCreateReq {
  /** 项目ID */
  private Long projectId;
  /** 用户ID */
  private Long userId;
  private String userName;
  /** 证书编号 */
  @NotBlank private String certificateNo;
  private String certificateType;
  /** 工种 */
  private String workType;
  /** 等级 */
  private String level;
  /** 颁发日期 */
  private String issueDate;
  private String expireDate;
  private String issuingAuthority;
  private String filePath;
  /** 状态 */
  private Integer status;
  /** 排序号 */
  private Integer sortNumber;

  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getUserName() { return userName; }
  public void setUserName(String userName) { this.userName = userName; }
  public String getCertificateNo() { return certificateNo; }
  public void setCertificateNo(String certificateNo) { this.certificateNo = certificateNo; }
  public String getCertificateType() { return certificateType; }
  public void setCertificateType(String certificateType) { this.certificateType = certificateType; }
  public String getWorkType() { return workType; }
  public void setWorkType(String workType) { this.workType = workType; }
  public String getLevel() { return level; }
  public void setLevel(String level) { this.level = level; }
  public String getIssueDate() { return issueDate; }
  public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
  public String getExpireDate() { return expireDate; }
  public void setExpireDate(String expireDate) { this.expireDate = expireDate; }
  public String getIssuingAuthority() { return issuingAuthority; }
  public void setIssuingAuthority(String issuingAuthority) { this.issuingAuthority = issuingAuthority; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
