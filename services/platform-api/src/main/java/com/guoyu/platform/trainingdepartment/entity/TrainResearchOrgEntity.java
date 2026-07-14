package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 培训调研企业/机构 (train_research_org)
 */
@TableName("train_research_org")
public class TrainResearchOrgEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  /** 企业/机构名称 */
  private String title;

  /** 对应 sys_org.id */
  private Long targetOrgId;

  /** 当前进行中的预报名批次 */
  private Long activeSignUpId;

  /** 已采集人数 */
  private Integer personnelCount;

  /** 人员采集字段下拉选项 JSON */
  private String collectionFieldOptions;

  /** 类型 */
  private String type;

  /** 省ID */
  private Integer provinceId;

  /** 省名称 */
  private String provinceName;

  /** 市ID */
  private Integer cityId;

  /** 市名称 */
  private String cityName;

  /** 区县ID */
  private Integer areaId;

  /** 区县名称 */
  private String areaName;

  /** 创建人 */
  private Long userId;

  /** 排序 */
  private Integer sortNumber;

  /** 状态 1-启用 0-禁用 */
  private Integer status;

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

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public Long getTargetOrgId() { return targetOrgId; }
  public void setTargetOrgId(Long targetOrgId) { this.targetOrgId = targetOrgId; }

  public Long getActiveSignUpId() { return activeSignUpId; }
  public void setActiveSignUpId(Long activeSignUpId) { this.activeSignUpId = activeSignUpId; }

  public Integer getPersonnelCount() { return personnelCount; }
  public void setPersonnelCount(Integer personnelCount) { this.personnelCount = personnelCount; }

  public String getCollectionFieldOptions() { return collectionFieldOptions; }
  public void setCollectionFieldOptions(String collectionFieldOptions) { this.collectionFieldOptions = collectionFieldOptions; }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }

  public Integer getProvinceId() { return provinceId; }
  public void setProvinceId(Integer provinceId) { this.provinceId = provinceId; }

  public String getProvinceName() { return provinceName; }
  public void setProvinceName(String provinceName) { this.provinceName = provinceName; }

  public Integer getCityId() { return cityId; }
  public void setCityId(Integer cityId) { this.cityId = cityId; }

  public String getCityName() { return cityName; }
  public void setCityName(String cityName) { this.cityName = cityName; }

  public Integer getAreaId() { return areaId; }
  public void setAreaId(Integer areaId) { this.areaId = areaId; }

  public String getAreaName() { return areaName; }
  public void setAreaName(String areaName) { this.areaName = areaName; }

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }

  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }

  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }

  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

  public LocalDateTime getUpdateTime() { return updateTime; }
  public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
