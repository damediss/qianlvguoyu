package com.guoyu.platform.certification.dto;

import com.guoyu.platform.common.api.PageQuery;

public class PreparationMaterialQueryReq extends PageQuery {
  private String status;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
