package com.guoyu.platform.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;

@TableName("exam_task_ximu_detail")
public class ExamTaskXimuDetailEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer taskId;
  private String sno;
  @TableField("sno_0")
  private String sno0;
  @TableField("sno_1")
  private String sno1;
  private String title;
  private String snoItem;
  private String snoTitle;
  private String rowColumn;
  private Integer tenantId;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  private LocalDateTime createTime;
  private LocalDateTime updatedAt;
  private Integer createdBy;
  private Integer updatedBy;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getTaskId() { return taskId; }
  public void setTaskId(Integer taskId) { this.taskId = taskId; }
  public String getSno() { return sno; }
  public void setSno(String sno) { this.sno = sno; }
  public String getSno0() { return sno0; }
  public void setSno0(String sno0) { this.sno0 = sno0; }
  public String getSno1() { return sno1; }
  public void setSno1(String sno1) { this.sno1 = sno1; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getSnoItem() { return snoItem; }
  public void setSnoItem(String snoItem) { this.snoItem = snoItem; }
  public String getSnoTitle() { return snoTitle; }
  public void setSnoTitle(String snoTitle) { this.snoTitle = snoTitle; }
  public String getRowColumn() { return rowColumn; }
  public void setRowColumn(String rowColumn) { this.rowColumn = rowColumn; }
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
