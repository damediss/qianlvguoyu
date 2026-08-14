package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_subsidy_pack_slot")
public class TrainSubsidyPackSlotEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long profileId;
  private String slotCode;
  private String title;
  private Integer requiredFlag;
  private Integer sortOrder;
  private String renderer;
  private Long templateId;
  private String dataBinding;
  private String folderPath;
  private String remark;
  @TableLogic
  private Integer deleted;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getProfileId() { return profileId; }
  public void setProfileId(Long profileId) { this.profileId = profileId; }
  public String getSlotCode() { return slotCode; }
  public void setSlotCode(String slotCode) { this.slotCode = slotCode; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Integer getRequiredFlag() { return requiredFlag; }
  public void setRequiredFlag(Integer requiredFlag) { this.requiredFlag = requiredFlag; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
  public String getRenderer() { return renderer; }
  public void setRenderer(String renderer) { this.renderer = renderer; }
  public Long getTemplateId() { return templateId; }
  public void setTemplateId(Long templateId) { this.templateId = templateId; }
  public String getDataBinding() { return dataBinding; }
  public void setDataBinding(String dataBinding) { this.dataBinding = dataBinding; }
  public String getFolderPath() { return folderPath; }
  public void setFolderPath(String folderPath) { this.folderPath = folderPath; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
