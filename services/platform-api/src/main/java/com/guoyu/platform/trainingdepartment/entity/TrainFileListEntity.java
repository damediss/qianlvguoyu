package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 文件管理
 */
@TableName("train_file_list")
public class TrainFileListEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 关联表名 */
  private String tableName;
  /** 关联主键 */
  private String pk;
  /** 文件标题 */
  private String title;
  /** 用户ID */
  private Long userId;
  /** 状态 */
  private Integer status;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getTableName() { return tableName; }
  public void setTableName(String tableName) { this.tableName = tableName; }
  public String getPk() { return pk; }
  public void setPk(String pk) { this.pk = pk; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
