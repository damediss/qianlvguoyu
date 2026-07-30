package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/**
 * 培训项目表单更新请求
 */
public class TrainProjectTableUpdateReq {
  /** 项目ID */
  private Long projectId;
  private Integer tableType;
  @Size(max = 255) private String tableName;
  /** 内容 */
  private String content;
  private Integer version;
  /** 状态 */
  private Integer status;
  private Long ownerId;
  @Size(max = 100) private String ownerName;
  /** 排序号 */
  private Integer sortNumber;

  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public Integer getTableType() { return tableType; }
  public void setTableType(Integer tableType) { this.tableType = tableType; }
  public String getTableName() { return tableName; }
  public void setTableName(String tableName) { this.tableName = tableName; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Long getOwnerId() { return ownerId; }
  public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
  public String getOwnerName() { return ownerName; }
  public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
