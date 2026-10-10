package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;

/** 按日从过程动态生成工作日志草稿 */
public class FollowWorkLogDraftFromOpsReq {

  @NotBlank
  private String bizDate;

  public String getBizDate() {
    return bizDate;
  }

  public void setBizDate(String bizDate) {
    this.bizDate = bizDate;
  }
}
