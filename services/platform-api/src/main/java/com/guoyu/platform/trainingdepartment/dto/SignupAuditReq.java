package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 批量审核报名请求 */
public class SignupAuditReq {

  @NotNull
  private List<Long> ids;

  /** 审核状态: 1通过, 2拒绝 */
  @NotNull
  /** 状态 */
  private Integer status;

  public List<Long> getIds() {
    return ids;
  }

  public void setIds(List<Long> ids) {
    this.ids = ids;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }
}
