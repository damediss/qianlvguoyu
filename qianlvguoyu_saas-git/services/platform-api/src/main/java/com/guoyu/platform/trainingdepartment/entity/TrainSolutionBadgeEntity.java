package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * train_solution_badge
 */
@TableName("train_solution_badge")
public class TrainSolutionBadgeEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 方案ID */
  private Long solutionId;

  /** 费用项目名称 */
  private String title;

  /** 费用金额 */
  private BigDecimal badge;

  /** 支出金额 */
  private BigDecimal expend;

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
  public Long getSolutionId() { return solutionId; }
  public void setSolutionId(Long solutionId) { this.solutionId = solutionId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public BigDecimal getBadge() { return badge; }
  public void setBadge(BigDecimal badge) { this.badge = badge; }
  public BigDecimal getExpend() { return expend; }
  public void setExpend(BigDecimal expend) { this.expend = expend; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
