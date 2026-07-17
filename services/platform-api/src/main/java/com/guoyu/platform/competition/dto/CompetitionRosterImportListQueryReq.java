package com.guoyu.platform.competition.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompetitionRosterImportListQueryReq extends PageQuery {

  private Long orgId;
  private String status;
  private String batchNo;

  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getBatchNo() { return batchNo; }
  public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
}
