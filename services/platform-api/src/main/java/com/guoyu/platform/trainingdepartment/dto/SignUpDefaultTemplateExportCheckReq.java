package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;

/** 查询默认模板导出进度 */
public class SignUpDefaultTemplateExportCheckReq {

  @NotNull
  private Long signUpId;

  @NotNull
  private Long taskId;

  private String signUpTitle;

  public Long getSignUpId() { return signUpId; }
  public void setSignUpId(Long signUpId) { this.signUpId = signUpId; }
  public Long getTaskId() { return taskId; }
  public void setTaskId(Long taskId) { this.taskId = taskId; }
  public String getSignUpTitle() { return signUpTitle; }
  public void setSignUpTitle(String signUpTitle) { this.signUpTitle = signUpTitle; }
}
