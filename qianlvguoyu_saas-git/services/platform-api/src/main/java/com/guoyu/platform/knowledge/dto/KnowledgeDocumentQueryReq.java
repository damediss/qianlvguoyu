package com.guoyu.platform.knowledge.dto;

import com.guoyu.platform.common.api.PageQuery;

public class KnowledgeDocumentQueryReq extends PageQuery {

  private String keyword;
  private String categoryCode;
  private String status;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getCategoryCode() {
    return categoryCode;
  }

  public void setCategoryCode(String categoryCode) {
    this.categoryCode = categoryCode;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
