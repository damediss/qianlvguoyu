package com.guoyu.platform.chazheng.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("chazheng_cert")
public class ChazhengCertEntity {

  @TableId(type = IdType.AUTO)
  private Integer id;

  private Integer userId;
  private String certNo;
  private String certName;
  private String certDate;
  private Long tenantId;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
  public String getCertNo() { return certNo; }
  public void setCertNo(String certNo) { this.certNo = certNo; }
  public String getCertName() { return certName; }
  public void setCertName(String certName) { this.certName = certName; }
  public String getCertDate() { return certDate; }
  public void setCertDate(String certDate) { this.certDate = certDate; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}