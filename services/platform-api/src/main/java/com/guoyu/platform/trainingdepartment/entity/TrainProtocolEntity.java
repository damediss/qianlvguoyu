package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 培训协议实体
 */
@TableName("train_protocol")
public class TrainProtocolEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 项目ID */
  private Long projectId;
  /** 标题 */
  private String title;
  /** 类型 */
  private String type;
  /** 内容 */
  private String content;
  private Integer version;
  /** 状态 */
  private Integer status;
  private String signedAt;
  private Long signerId;
  private Long approverId;
  private String rejectReason;
  /** 排序号 */
  private Integer sortNumber;
  /** 租户ID */
  private Long tenantId;
  /** 用户ID */
  private Long userId;
  /** 创建时间 */
  private LocalDateTime createTime;
  /** 更新时间 */
  private LocalDateTime updateTime;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getProjectId() { return projectId; }
  public void setProjectId(Long projectId) { this.projectId = projectId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getSignedAt() { return signedAt; }
  public void setSignedAt(String signedAt) { this.signedAt = signedAt; }
  public Long getSignerId() { return signerId; }
  public void setSignerId(Long signerId) { this.signerId = signerId; }
  public Long getApproverId() { return approverId; }
  public void setApproverId(Long approverId) { this.approverId = approverId; }
  public String getRejectReason() { return rejectReason; }
  public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
