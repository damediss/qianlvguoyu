package com.guoyu.platform.certification.info.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CertificationInfoQueryReq extends PageQuery {
  private String keyword;
  private String auditStatus;
  private String status;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getAuditStatus() {
    return auditStatus;
  }

  public void setAuditStatus(String auditStatus) {
    this.auditStatus = auditStatus;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
