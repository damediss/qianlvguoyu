package com.guoyu.platform.chazheng.vo;

import com.guoyu.platform.chazheng.entity.ChazhengCertEntity;

public class ChazhengCertVO {

  private String certNo;
  private String certName;
  private String certDate;
  /** 证书图片文件ID */
  private String licenseFileId;

  public static ChazhengCertVO from(ChazhengCertEntity entity) {
    ChazhengCertVO vo = new ChazhengCertVO();
    vo.setCertNo(entity.getCertNo());
    vo.setCertName(entity.getCertName());
    vo.setCertDate(entity.getCertDate());
    vo.setLicenseFileId(entity.getLicenseFileId());
    return vo;
  }

  public String getCertNo() { return certNo; }
  public void setCertNo(String certNo) { this.certNo = certNo; }
  public String getCertName() { return certName; }
  public void setCertName(String certName) { this.certName = certName; }
  public String getCertDate() { return certDate; }
  public void setCertDate(String certDate) { this.certDate = certDate; }
  public String getLicenseFileId() { return licenseFileId; }
  public void setLicenseFileId(String licenseFileId) { this.licenseFileId = licenseFileId; }
}