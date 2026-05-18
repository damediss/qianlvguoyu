package com.guoyu.platform.trainingdepartment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("train_declare")
public class TrainDeclareEntity {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long trainId;
  private String title;
  private Long cityId;
  private String cityName;
  private Long userId;
  private Integer status;
  private Integer sortNumber;
  private Long tenantId;
  private LocalDateTime createTime;
  @TableLogic
  private Integer deleted;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getTrainId() { return trainId; }
  public void setTrainId(Long trainId) { this.trainId = trainId; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public Long getCityId() { return cityId; }
  public void setCityId(Long cityId) { this.cityId = cityId; }
  public String getCityName() { return cityName; }
  public void setCityName(String cityName) { this.cityName = cityName; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
  public LocalDateTime getCreateTime() { return createTime; }
  public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
  public Integer getDeleted() { return deleted; }
  public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
