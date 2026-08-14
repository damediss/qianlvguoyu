package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_subsidy_slot_file")
public class TrainSubsidySlotFileEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long subsidyId;
  private String slotCode;
  private Long fileId;
  private String filePath;
  private String fileName;
  private Long uploadedBy;
  private Long tenantId;
  @TableLogic
  private Integer deleted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getSubsidyId() { return subsidyId; }
  public void setSubsidyId(Long subsidyId) { this.subsidyId = subsidyId; }
  public String getSlotCode() { return slotCode; }
  public void setSlotCode(String slotCode) { this.slotCode = slotCode; }
  public Long getFileId() { return fileId; }
  public void setFileId(Long fileId) { this.fileId = fileId; }
  public String getFilePath() { return filePath; }
  public void setFilePath(String filePath) { this.filePath = filePath; }
  public String getFileName() { return fileName; }
  public void setFileName(String fileName) { this.fileName = fileName; }
  public Long getUploadedBy() { return uploadedBy; }
  public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
