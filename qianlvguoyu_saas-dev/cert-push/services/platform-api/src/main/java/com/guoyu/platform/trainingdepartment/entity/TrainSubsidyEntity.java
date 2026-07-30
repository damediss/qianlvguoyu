package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培训补贴实体
 */
@TableName("train_subsidy")
public class TrainSubsidyEntity {

  /**  主键ID */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 项目ID */
  private Long projectId;
  private String policyTemplate;
  /** 补贴金额 */
  private BigDecimal subsidyAmount;
  private Integer studentCount;
  private String materialPackagePath;
  private Integer materialStatus;
  private Integer rssbStatus;
  private String rssbSubmitTime;
  private String rssbReceiptNo;
  private String rssbRejectReason;
  private BigDecimal grantedAmount;
  private String grantTime;
  /** 备注 */
  private String remark;
  /** 状态 */
  private Integer status;
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
  public String getPolicyTemplate() { return policyTemplate; }
  public void setPolicyTemplate(String policyTemplate) { this.policyTemplate = policyTemplate; }
  public BigDecimal getSubsidyAmount() { return subsidyAmount; }
  public void setSubsidyAmount(BigDecimal subsidyAmount) { this.subsidyAmount = subsidyAmount; }
  public Integer getStudentCount() { return studentCount; }
  public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }
  public String getMaterialPackagePath() { return materialPackagePath; }
  public void setMaterialPackagePath(String materialPackagePath) { this.materialPackagePath = materialPackagePath; }
  public Integer getMaterialStatus() { return materialStatus; }
  public void setMaterialStatus(Integer materialStatus) { this.materialStatus = materialStatus; }
  public Integer getRssbStatus() { return rssbStatus; }
  public void setRssbStatus(Integer rssbStatus) { this.rssbStatus = rssbStatus; }
  public String getRssbSubmitTime() { return rssbSubmitTime; }
  public void setRssbSubmitTime(String rssbSubmitTime) { this.rssbSubmitTime = rssbSubmitTime; }
  public String getRssbReceiptNo() { return rssbReceiptNo; }
  public void setRssbReceiptNo(String rssbReceiptNo) { this.rssbReceiptNo = rssbReceiptNo; }
  public String getRssbRejectReason() { return rssbRejectReason; }
  public void setRssbRejectReason(String rssbRejectReason) { this.rssbRejectReason = rssbRejectReason; }
  public BigDecimal getGrantedAmount() { return grantedAmount; }
  public void setGrantedAmount(BigDecimal grantedAmount) { this.grantedAmount = grantedAmount; }
  public String getGrantTime() { return grantTime; }
  public void setGrantTime(String grantTime) { this.grantTime = grantTime; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
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
