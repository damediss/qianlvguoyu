package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * train_sign_up_orc_self
 */
@TableName("train_sign_up_orc_self")
public class TrainSignUpOrcSelfEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 报名计划ID */
  private Long signUpId;

  /** 平台用户ID */
  private Long platformUserId;

  /** OCR识别原始数据 */
  private String orcData;

  /** 状态 0-失败 1-成功 */
  private Integer status;

  /** 失败原因 */
  private String failReason;

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
  public Long getSignUpId() { return signUpId; }
  public void setSignUpId(Long signUpId) { this.signUpId = signUpId; }
  public Long getPlatformUserId() { return platformUserId; }
  public void setPlatformUserId(Long platformUserId) { this.platformUserId = platformUserId; }
  public String getOrcData() { return orcData; }
  public void setOrcData(String orcData) { this.orcData = orcData; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public String getFailReason() { return failReason; }
  public void setFailReason(String failReason) { this.failReason = failReason; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
