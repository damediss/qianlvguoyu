package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * demand_train
 */
@TableName("demand_train")
public class DemandTrainEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 企业ID */
  private Long orgId;

  /** 需求内容 */
  private String content;

  /** 人数 */
  private Integer nums;

  /** 状态 0-待处理 1-已处理 */
  private Integer status;

  /** 添加时间戳 */
  private Integer addtime;

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
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }
  public Integer getNums() { return nums; }
  public void setNums(Integer nums) { this.nums = nums; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getAddtime() { return addtime; }
  public void setAddtime(Integer addtime) { this.addtime = addtime; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
