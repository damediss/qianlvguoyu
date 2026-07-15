package com.guoyu.platform.chazheng.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.time.LocalDateTime;

/**
 * 查证批次实体。
 *
 * <p>每次导入或手动触发查证时创建一个批次记录，追踪整体进度。
 */
@TableName("chazheng_cert_batch")
public class CertCheckBatchEntity extends BaseEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private Long tenantId;
  private Integer totalCount = 0;
  private Integer completedCount = 0;
  private String status = "processing";
  private String sourceFilename;
  private Long triggerBy;
  private LocalDateTime triggeredAt;
  private LocalDateTime completedAt;
  private String errorMessage;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // ── getter/setter ──

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Integer getTotalCount() { return totalCount; }
  public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
  public Integer getCompletedCount() { return completedCount; }
  public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getSourceFilename() { return sourceFilename; }
  public void setSourceFilename(String sourceFilename) { this.sourceFilename = sourceFilename; }
  public Long getTriggerBy() { return triggerBy; }
  public void setTriggerBy(Long triggerBy) { this.triggerBy = triggerBy; }
  public LocalDateTime getTriggeredAt() { return triggeredAt; }
  public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }
  public LocalDateTime getCompletedAt() { return completedAt; }
  public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
