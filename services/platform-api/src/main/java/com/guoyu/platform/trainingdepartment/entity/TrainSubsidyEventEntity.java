package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_subsidy_event")
public class TrainSubsidyEventEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long subsidyId;
  private String action;
  private String fromPhase;
  private String toPhase;
  private String remark;
  private String payloadJson;
  private Long operatorId;
  private Long tenantId;
  @TableLogic
  private Integer deleted;
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getSubsidyId() { return subsidyId; }
  public void setSubsidyId(Long subsidyId) { this.subsidyId = subsidyId; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public String getFromPhase() { return fromPhase; }
  public void setFromPhase(String fromPhase) { this.fromPhase = fromPhase; }
  public String getToPhase() { return toPhase; }
  public void setToPhase(String toPhase) { this.toPhase = toPhase; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public String getPayloadJson() { return payloadJson; }
  public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
  public Long getOperatorId() { return operatorId; }
  public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
