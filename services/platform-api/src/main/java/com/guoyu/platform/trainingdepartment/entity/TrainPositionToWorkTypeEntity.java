package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * train_position_to_work_type
 */
@TableName("train_position_to_work_type")
public class TrainPositionToWorkTypeEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 岗位名称 */
  private String positionName;

  /** 工种ID */
  private Long workTypeId;

  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 更新时间 */
  private LocalDateTime updateTime;

  /** 逻辑删除 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;
  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getPositionName() { return positionName; }
  public void setPositionName(String positionName) { this.positionName = positionName; }
  public Long getWorkTypeId() { return workTypeId; }
  public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
