package com.guoyu.platform.competition.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CompetitionNoticeQueryReq extends PageQuery {

  private String status;
  private String contentType;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }
}
