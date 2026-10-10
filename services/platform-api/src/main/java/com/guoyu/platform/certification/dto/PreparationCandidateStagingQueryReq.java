package com.guoyu.platform.certification.dto;

import com.guoyu.platform.common.api.PageQuery;

public class PreparationCandidateStagingQueryReq extends PageQuery {

  private String keyword;
  private String promoteStatus;
  private String sourceType;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getPromoteStatus() {
    return promoteStatus;
  }

  public void setPromoteStatus(String promoteStatus) {
    this.promoteStatus = promoteStatus;
  }

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }
}
