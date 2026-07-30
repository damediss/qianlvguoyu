package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * train_base_data
 */
@TableName("train_base_data")
public class TrainBaseDataEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 配置键 */
  private String dataKey;

  /** 配置值 */
  private String dataValue;

  /** 数据类型 */
  private String dataType;

  /** 描述 */
  private String description;

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
  public String getDataKey() { return dataKey; }
  public void setDataKey(String dataKey) { this.dataKey = dataKey; }
  public String getDataValue() { return dataValue; }
  public void setDataValue(String dataValue) { this.dataValue = dataValue; }
  public String getDataType() { return dataType; }
  public void setDataType(String dataType) { this.dataType = dataType; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
