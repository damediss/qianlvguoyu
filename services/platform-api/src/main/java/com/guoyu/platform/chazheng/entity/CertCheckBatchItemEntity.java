package com.guoyu.platform.chazheng.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

/**
 * 查证明细实体。
 *
 * <p>批次中每个用户的查证记录，追踪单个用户的查证状态和错误信息。
 */
@TableName("chazheng_cert_batch_item")
public class CertCheckBatchItemEntity extends BaseEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private Long batchId;
  private Long userId;
  private String userName;
  private String status = "pending";
  private Integer retryCount = 0;
  private String errorMessage;

  // ── getter/setter ──

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getBatchId() { return batchId; }
  public void setBatchId(Long batchId) { this.batchId = batchId; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getUserName() { return userName; }
  public void setUserName(String userName) { this.userName = userName; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Integer getRetryCount() { return retryCount; }
  public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
  public String getErrorMessage() { return errorMessage; }
  public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
