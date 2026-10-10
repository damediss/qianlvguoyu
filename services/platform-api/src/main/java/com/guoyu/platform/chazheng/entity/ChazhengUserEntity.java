package com.guoyu.platform.chazheng.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("chazheng_user")
public class ChazhengUserEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer enterpriseId;
  private String userName;
  private String idCard;
  private Integer gender;
  private String datas;
  private Integer isCert;
  private Integer isChecked;
  private String ymd;
  private Integer status;
  private Integer sortNumber;
  private Long tenantId;
  private LocalDateTime createTime;

  @TableLogic(value = "0", delval = "1")
  private Integer deleted;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getEnterpriseId() { return enterpriseId; }
  public void setEnterpriseId(Integer enterpriseId) { this.enterpriseId = enterpriseId; }
  public String getUserName() { return userName; }
  public void setUserName(String userName) { this.userName = userName; }
  public String getIdCard() { return idCard; }
  public void setIdCard(String idCard) { this.idCard = idCard; }
  public Integer getGender() { return gender; }
  public void setGender(Integer gender) { this.gender = gender; }
  public String getDatas() { return datas; }
  public void setDatas(String datas) { this.datas = datas; }
  public Integer getIsCert() { return isCert; }
  public void setIsCert(Integer isCert) { this.isCert = isCert; }
  public Integer getIsChecked() { return isChecked; }
  public void setIsChecked(Integer isChecked) { this.isChecked = isChecked; }
  public String getYmd() { return ymd; }
  public void setYmd(String ymd) { this.ymd = ymd; }
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