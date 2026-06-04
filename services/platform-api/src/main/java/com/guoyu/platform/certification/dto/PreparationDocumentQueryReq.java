package com.guoyu.platform.certification.dto;

import com.guoyu.platform.common.api.PageQuery;

public class PreparationDocumentQueryReq extends PageQuery {
  private Long candidateId;
  private String keyword;
  private String status;

  public Long getCandidateId() {
    return candidateId;
  }

  public void setCandidateId(Long candidateId) {
    this.candidateId = candidateId;
  }

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
}
