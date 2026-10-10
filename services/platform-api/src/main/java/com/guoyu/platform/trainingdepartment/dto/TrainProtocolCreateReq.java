package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/**
 * 培训协议创建请求
 */
public class TrainProtocolCreateReq {
  /** 项目ID */
  private Long projectId;
  /** 标题 */
  @Size(max = 255) private String title;
  /** 类型 */
  @Size(max = 100) private String type;
  /** 内容 */
  private String content;
  private Integer version;
  /** 状态 */
  private Integer status;
  private String signedAt;
  private Long signerId;
  private Long approverId;
  @Size(max = 500) private String rejectReason;
  /** 排序号 */
  private Integer sortNumber;

  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getSignedAt() { return signedAt; }
  public void setSignedAt(String signedAt) { this.signedAt = signedAt; }
  public Long getSignerId() { return signerId; }
  public void setSignerId(Long signerId) { this.signerId = signerId; }
  public Long getApproverId() { return approverId; }
  public void setApproverId(Long approverId) { this.approverId = approverId; }
  public String getRejectReason() { return rejectReason; }
  public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
