package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * demand_employee
 */
@TableName("demand_employee")
public class DemandEmployeeEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 企业ID */
  private Long orgId;

  /** 工种/岗位 */
  private String work;

  /** 学历要求 */
  private String education;

  /** 专业要求 */
  private String major;

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
  public String getWork() { return work; }
  public void setWork(String work) { this.work = work; }
  public String getEducation() { return education; }
  public void setEducation(String education) { this.education = education; }
  public String getMajor() { return major; }
  public void setMajor(String major) { this.major = major; }
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
