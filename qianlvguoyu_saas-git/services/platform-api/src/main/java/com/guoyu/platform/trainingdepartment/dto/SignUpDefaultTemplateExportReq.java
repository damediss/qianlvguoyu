package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 启动默认模板导出 */
public class SignUpDefaultTemplateExportReq {

  @NotNull
  private Long signUpId;

  private List<Long> ids;
  private Integer status;

  public Long getSignUpId() { return signUpId; }
  public void setSignUpId(Long signUpId) { this.signUpId = signUpId; }
  public List<Long> getIds() { return ids; }
  public void setIds(List<Long> ids) { this.ids = ids; }
  public Integer getStatus() { return status; }
  public void setStatus(Integer status) { this.status = status; }
}
