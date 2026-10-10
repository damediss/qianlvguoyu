package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 分班
 */
@TableName("train_class_list")
public class TrainClassListEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 培训ID */
  private Long trainId;
  /** 用户ID */
  private Long userId;
  /** 是否组长 0-否 1-是 */
  private Integer isLeader;
  /** 是否班委 0-否 1-是 */
  private Integer isCommittee;
  /** 班级名称 */
  private String className;
  /** 排序号 */
  private Integer sortNumber;
  /** 状态 0-未开班 1-开班 2-结课 */
  private Integer status;
  /** 租户ID */
  private Long tenantId;
  /** 创建时间 */
  private LocalDateTime createTime;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Integer getIsLeader() { return isLeader; }
  public void setIsLeader(Integer isLeader) { this.isLeader = isLeader; }
  public Integer getIsCommittee() { return isCommittee; }
  public void setIsCommittee(Integer isCommittee) { this.isCommittee = isCommittee; }
  public String getClassName() { return className; }
  public void setClassName(String className) { this.className = className; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
