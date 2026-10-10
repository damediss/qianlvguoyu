package com.guoyu.platform.chazheng.vo;

import com.guoyu.platform.chazheng.entity.ChazhengCertEntity;

public class ChazhengCertVO {

  private String certNo;
  private String certName;
  private String certDate;

  public static ChazhengCertVO from(ChazhengCertEntity entity) {
    ChazhengCertVO vo = new ChazhengCertVO();
    vo.setCertNo(entity.getCertNo());
    vo.setCertName(entity.getCertName());
    vo.setCertDate(entity.getCertDate());
    return vo;
  }

  public String getCertNo() { return certNo; }
  public void setCertNo(String certNo) { this.certNo = certNo; }
  public String getCertName() { return certName; }
  public void setCertName(String certName) { this.certName = certName; }
  public String getCertDate() { return certDate; }
  public void setCertDate(String certDate) { this.certDate = certDate; }
}