package com.guoyu.platform.system.dto;

import com.guoyu.platform.common.api.PageQuery;

public class RoleQueryReq extends PageQuery {

  private String keyword;
  private String roleType;
  private String status;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getRoleType() {
    return roleType;
  }

  public void setRoleType(String roleType) {
    this.roleType = roleType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
