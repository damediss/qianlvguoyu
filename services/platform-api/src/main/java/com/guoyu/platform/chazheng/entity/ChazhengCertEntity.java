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
  /** 证书图片文件ID（licensefile base64 解码存文件存储，库里只存 ID） */
  private String licenseFileId;
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
  public String getLicenseFileId() { return licenseFileId; }
  public void setLicenseFileId(String licenseFileId) { this.licenseFileId = licenseFileId; }
  public Long getTenantId() { return tenantId; }
  public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
}