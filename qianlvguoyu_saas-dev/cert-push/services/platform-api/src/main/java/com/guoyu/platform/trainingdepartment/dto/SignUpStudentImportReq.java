package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 从学员库导入报名人员 */
public class SignUpStudentImportReq {

  @NotNull
  private Long signUpId;

  @NotEmpty
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
