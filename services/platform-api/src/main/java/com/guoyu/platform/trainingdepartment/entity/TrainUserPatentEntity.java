package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 专利
 */
@TableName("train_user_patent")
public class TrainUserPatentEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 专利标题 */
  private String title;
  /** 专利内容/URL */
  private String paper;
  /** 用户ID */
  private Long userId;
  /** 租户ID */
  private Long tenantId;
  /** 更新时间 */
  private LocalDateTime updateTime;
  /** 创建时间 */
  private LocalDateTime createTime;

  /**  逻辑删除标记 0-未删除 1-已删除 */
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getPaper() { return paper; }
  public void setPaper(String paper) { this.paper = paper; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
