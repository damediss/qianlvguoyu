package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_subsidy_material_version")
public class TrainSubsidyMaterialVersionEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long instanceId;
  private Integer versionNo;
  private Long fileId;
  private String filePath;
  private String fileName;
  private String source;
  private String fieldSnapshotJson;
  private Integer confirmed;
  private Long confirmedBy;
  private LocalDateTime confirmedAt;
  private String remark;
  private Long createdBy;
  private Long tenantId;
  @TableLogic
  private Integer deleted;
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getInstanceId() { return instanceId; }
  public void setInstanceId(Long instanceId) { this.instanceId = instanceId; }
  public Integer getVersionNo() { return versionNo; }
  public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
  public Long getFileId() { return fileId; }
  public void setFileId(Long fileId) { this.fileId = fileId; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public String getFileName() { return fileName; }
  public void setFileName(String fileName) { this.fileName = fileName; }
  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }
  public String getFieldSnapshotJson() { return fieldSnapshotJson; }
  public void setFieldSnapshotJson(String fieldSnapshotJson) { this.fieldSnapshotJson = fieldSnapshotJson; }
  public Integer getConfirmed() { return confirmed; }
  public void setConfirmed(Integer confirmed) { this.confirmed = confirmed; }
  public Long getConfirmedBy() { return confirmedBy; }
  public void setConfirmedBy(Long confirmedBy) { this.confirmedBy = confirmedBy; }
  public LocalDateTime getConfirmedAt() { return confirmedAt; }
  public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public Long getCreatedBy() { return createdBy; }
  public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
