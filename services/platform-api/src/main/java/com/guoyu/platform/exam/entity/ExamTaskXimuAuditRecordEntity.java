package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("task_ximu_audit_records")
public class ExamTaskXimuAuditRecordEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer expertId;
  private Integer auditRole;
  private Integer taskId;
  private Integer ximuId;
  private String sno;
  private String snoItem;
  private String snoTitle;
  private Integer auditStatus;
  private String ymd;
  private LocalDateTime addtime;
  private Integer tenantId;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  private LocalDateTime createTime;
  private LocalDateTime updatedAt;
  private Integer createdBy;
  private Integer updatedBy;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getExpertId() { return expertId; }
  public void setExpertId(Integer expertId) { this.expertId = expertId; }
  public Integer getAuditRole() { return auditRole; }
  public void setAuditRole(Integer auditRole) { this.auditRole = auditRole; }
  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public Integer getXimuId() { return ximuId; }
  public void setXimuId(Integer ximuId) { this.ximuId = ximuId; }
  public String getSno() { return sno; }
  public void setSno(String sno) { this.sno = sno; }
  public String getSnoItem() { return snoItem; }
  public void setSnoItem(String snoItem) { this.snoItem = snoItem; }
  public String getSnoTitle() { return snoTitle; }
  public void setSnoTitle(String snoTitle) { this.snoTitle = snoTitle; }
  public Integer getAuditStatus() { return auditStatus; }
  public void setAuditStatus(Integer auditStatus) { this.auditStatus = auditStatus; }
  public String getYmd() { return ymd; }
  public void setYmd(String ymd) { this.ymd = ymd; }
  public LocalDateTime getAddtime() { return addtime; }
  public void setAddtime(LocalDateTime addtime) { this.addtime = addtime; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public Integer getCreatedBy() { return createdBy; }
  public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
  public Integer getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Integer updatedBy) { this.updatedBy = updatedBy; }
}
