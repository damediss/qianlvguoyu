package com.guoyu.platform.chazheng.service;

/**
 * 从接口单条证响应中提取的待写入数据。
 */
public class CollectedCert {
  public final String certNo;
  public final String certName;
  public final String certDate;

  public CollectedCert(String certNo, String certName, String certDate) {
    this.certNo = certNo;
    this.certName = certName;
    this.certDate = certDate;
  }
}
