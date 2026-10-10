package com.guoyu.platform.trainingdepartment.dto;

/**
 * 培训档案更新请求
 */
public class TrainArchiveUpdateReq {
  /** 项目ID */
  private Long projectId;
  private String archiveType;
  private Integer fileCount;
  private String archiveTime;
  private String filePaths;
  /** 状态 */
  private Integer status;
  private Long approverId;
  private String approverName;
  /** 审批时间 */
  private String approveTime;
  private String rejectReason;
  /** 备注 */
  private String remark;
  /** 排序号 */
  private Integer sortNumber;

  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public String getArchiveType() { return archiveType; }
  public void setArchiveType(String archiveType) { this.archiveType = archiveType; }
  public Integer getFileCount() { return fileCount; }
  public void setFileCount(Integer fileCount) { this.fileCount = fileCount; }
  public String getArchiveTime() { return archiveTime; }
  public void setArchiveTime(String archiveTime) { this.archiveTime = archiveTime; }
  public String getFilePaths() { return filePaths; }
  public void setFilePaths(String filePaths) { this.filePaths = filePaths; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Long getApproverId() { return approverId; }
  public void setApproverId(Long approverId) { this.approverId = approverId; }
  public String getApproverName() { return approverName; }
  public void setApproverName(String approverName) { this.approverName = approverName; }
  public String getApproveTime() { return approveTime; }
  public void setApproveTime(String approveTime) { this.approveTime = approveTime; }
  public String getRejectReason() { return rejectReason; }
  public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
