package com.guoyu.platform.certification.dto;

import com.guoyu.platform.common.api.PageQuery;

public class PreparationCandidateQueryReq extends PageQuery {
  private String keyword;
  private String status;
  private String levelCode;
  private Boolean documentComplete;

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

  public String getLevelCode() {
    return levelCode;
  }

  public void setLevelCode(String levelCode) {
    this.levelCode = levelCode;
  }

  public Boolean getDocumentComplete() {
    return documentComplete;
  }

  public void setDocumentComplete(Boolean documentComplete) {
    this.documentComplete = documentComplete;
  }
}
