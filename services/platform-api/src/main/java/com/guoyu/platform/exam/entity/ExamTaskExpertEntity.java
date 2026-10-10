package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 通用任务专家分配 —— 见 V20260602_001。
 * task_type ∈ {ximu, exam, examshicao, examsub}。
 */
@TableName("task_expert")
public class ExamTaskExpertEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer taskId;
  private String taskType;
  private Integer expertId;
  private Integer taskRole;
  private String tag;
  private LocalDateTime addtime;
  private LocalDateTime endtime;
  private Integer tenantId;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Integer createdBy;
  private Integer updatedBy;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public String getTaskType() { return taskType; }
  public void setTaskType(String taskType) { this.taskType = taskType; }
  public Integer getExpertId() { return expertId; }
  public void setExpertId(Integer expertId) { this.expertId = expertId; }
  public Integer getTaskRole() { return taskRole; }
  public void setTaskRole(Integer taskRole) { this.taskRole = taskRole; }
  public String getTag() { return tag; }
  public void setTag(String tag) { this.tag = tag; }
  public LocalDateTime getAddtime() { return addtime; }
  public void setAddtime(LocalDateTime addtime) { this.addtime = addtime; }
  public LocalDateTime getEndtime() { return endtime; }
  public void setEndtime(LocalDateTime endtime) { this.endtime = endtime; }
  public Integer getTenantId() { return tenantId; }
  public void setTenantId(Integer tenantId) { this.tenantId = tenantId; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
  public Integer getCreatedBy() { return createdBy; }
  public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
  public Integer getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Integer updatedBy) { this.updatedBy = updatedBy; }
}
