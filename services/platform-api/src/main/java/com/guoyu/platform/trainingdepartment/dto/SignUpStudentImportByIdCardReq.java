package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 按身份证号从学员库导入报名人员 */
public class SignUpStudentImportByIdCardReq {

  @NotNull
  private Long signUpId;

  /** 多行身份证，一行一个（也支持逗号/空白分隔） */
  @NotBlank
  private String idCards;

  public Long getSignUpId() {
    return signUpId;
  }

  public void setSignUpId(Long signUpId) {
    this.signUpId = signUpId;
  }

  public String getIdCards() {
    return idCards;
  }

  public void setIdCards(String idCards) {
    this.idCards = idCards;
  }
}
