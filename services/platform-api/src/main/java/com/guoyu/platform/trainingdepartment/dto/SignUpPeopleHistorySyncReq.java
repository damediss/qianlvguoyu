package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 资料收集：按身份证从历史信息收集/扫码填报同步到当前计划 */
public class SignUpPeopleHistorySyncReq {

  @NotNull
  private Long signUpId;

  /** 可选：当前计划下的人员 ID；空则处理本计划全部有身份证的人 */
  private List<Long> ids;

  public Long getSignUpId() {
    return signUpId;
  }

  public void setSignUpId(Long signUpId) {
    this.signUpId = signUpId;
  }

  public List<Long> getIds() {
    return ids;
  }

  public void setIds(List<Long> ids) {
    this.ids = ids;
  }
}
