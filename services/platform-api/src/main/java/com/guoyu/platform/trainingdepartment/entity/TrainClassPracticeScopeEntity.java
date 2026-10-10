package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_class_practice_scope")
public class TrainClassPracticeScopeEntity {

  @TableId(type = IdType.AUTO)
  private Long id;
  private Long tenantId;
  private Long classId;
  private Integer enabled;
  private String remark;
  /** 刷题开放开始；空=不限 */
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private LocalDateTime practiceStartTime;
  /** 刷题截止；空=不过期 */
  @TableField(updateStrategy = FieldStrategy.ALWAYS)
  private LocalDateTime practiceEndTime;
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
  public Integer getEnabled() { return enabled; }
  public void setEnabled(Integer enabled) { this.enabled = enabled; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public LocalDateTime getPracticeStartTime() { return practiceStartTime; }
  public void setPracticeStartTime(LocalDateTime practiceStartTime) { this.practiceStartTime = practiceStartTime; }
  public LocalDateTime getPracticeEndTime() { return practiceEndTime; }
  public void setPracticeEndTime(LocalDateTime practiceEndTime) { this.practiceEndTime = practiceEndTime; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
