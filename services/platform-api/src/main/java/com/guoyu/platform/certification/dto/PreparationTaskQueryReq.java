package com.guoyu.platform.certification.dto;

import com.guoyu.platform.common.api.PageQuery;

public class PreparationTaskQueryReq extends PageQuery {
  private String keyword;
  private String status;
  private String currentPhase;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCurrentPhase() {
    return currentPhase;
  }

  public void setCurrentPhase(String currentPhase) {
    this.currentPhase = currentPhase;
  }
}
