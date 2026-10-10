package com.guoyu.platform.system.dto;

import com.guoyu.platform.common.api.PageQuery;

public class AuditLogQueryReq extends PageQuery {

  private String module;
  private String action;
  private String targetType;
  private Long userId;

  public String getModule() {
    return module;
  }

  public void setModule(String module) {
    this.module = module;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }
}
