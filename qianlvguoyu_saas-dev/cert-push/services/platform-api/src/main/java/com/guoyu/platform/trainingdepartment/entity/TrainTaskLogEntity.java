package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * train_task_log
 */
@TableName("train_task_log")
public class TrainTaskLogEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 班级ID */
  private Long trainClassId;

  /** 任务检查点ID */
  private Long ckId;

  /** 日期 */
  private String date;

  /** 添加时间戳 */
  private Integer addtime;

  /** 状态 */
  private Integer status;

  /** 时间范围 */
  private String times;

  /** 租户ID（PHP 遗留表无此列） */
  @TableField(exist = false)
  private Long tenantId;
  /** 创建时间（PHP 遗留表无此列） */
  @TableField(exist = false)
  private LocalDateTime createTime;
  /** 更新时间（PHP 遗留表无此列） */
  @TableField(exist = false)
  private LocalDateTime updateTime;

  /** 逻辑删除 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTrainClassId() { return trainClassId; }
  public void setTrainClassId(Long trainClassId) { this.trainClassId = trainClassId; }
  public Long getCkId() { return ckId; }
  public void setCkId(Long ckId) { this.ckId = ckId; }
  public String getDate() { return date; }
  public void setDate(String date) { this.date = date; }
  public Integer getAddtime() { return addtime; }
  public void setAddtime(Integer addtime) { this.addtime = addtime; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getTimes() { return times; }
  public void setTimes(String times) { this.times = times; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
