package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_subsidy_material_instance")
public class TrainSubsidyMaterialInstanceEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long subsidyId;
  private String slotCode;
  private Long templateId;
  private String materialType;
  private String title;
  private String status;
  private Long currentVersionId;
  private Integer requiredFlag;
  private Integer sortOrder;
  private String renderer;
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
  public Long getTemplateId() { return templateId; }
  public void setTemplateId(Long templateId) { this.templateId = templateId; }
  public String getMaterialType() { return materialType; }
  public void setMaterialType(String materialType) { this.materialType = materialType; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getCurrentVersionId() { return currentVersionId; }
  public void setCurrentVersionId(Long currentVersionId) { this.currentVersionId = currentVersionId; }
  public Integer getRequiredFlag() { return requiredFlag; }
  public void setRequiredFlag(Integer requiredFlag) { this.requiredFlag = requiredFlag; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public String getRenderer() { return renderer; }
  public void setRenderer(String renderer) { this.renderer = renderer; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
