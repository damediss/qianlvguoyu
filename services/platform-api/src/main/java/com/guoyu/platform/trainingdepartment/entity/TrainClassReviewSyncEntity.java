package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 班级同步学配置（通道 C：class → exam 绑定） */
@TableName("train_class_review_sync")
public class TrainClassReviewSyncEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long tenantId;
  private Long classId;
  private Long examId;
  private Integer enabled;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private LocalDateTime syncStartTime;
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private LocalDateTime syncEndTime;
  private Integer autoEnroll;
  private String remark;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getClassId() { return classId; }
  public void setClassId(Long classId) { this.classId = classId; }
  public Long getExamId() { return examId; }
  public void setExamId(Long examId) { this.examId = examId; }
  public Integer getEnabled() { return enabled; }
  public void setEnabled(Integer enabled) { this.enabled = enabled; }
  public LocalDateTime getSyncStartTime() { return syncStartTime; }
  public void setSyncStartTime(LocalDateTime syncStartTime) { this.syncStartTime = syncStartTime; }
  public LocalDateTime getSyncEndTime() { return syncEndTime; }
  public void setSyncEndTime(LocalDateTime syncEndTime) { this.syncEndTime = syncEndTime; }
  public Integer getAutoEnroll() { return autoEnroll; }
  public void setAutoEnroll(Integer autoEnroll) { this.autoEnroll = autoEnroll; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
