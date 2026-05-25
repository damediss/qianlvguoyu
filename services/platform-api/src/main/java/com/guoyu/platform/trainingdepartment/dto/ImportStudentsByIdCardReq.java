package com.guoyu.platform.trainingdepartment.dto;

/** 按身份证号导入学员请求 */
public class ImportStudentsByIdCardReq {

  private String idCards;

  public String getIdCards() { return idCards; }
  public void setIdCards(String idCards) { this.idCards = idCards; }
}
