package com.guoyu.platform.certification.dto;

import com.guoyu.platform.common.api.PageQuery;

public class CertificationTaskQueryReq extends PageQuery {

  private String keyword;
  private String status;
  private String currentPhase;
  private String levelCode;
  private String taskNo;

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCurrentPhase() {
    return currentPhase;
  }

  public void setCurrentPhase(String currentPhase) {
    this.currentPhase = currentPhase;
  }

  public String getLevelCode() {
    return levelCode;
  }

  public void setLevelCode(String levelCode) {
    this.levelCode = levelCode;
  }

  public String getTaskNo() {
    return taskNo;
  }

  public void setTaskNo(String taskNo) {
    this.taskNo = taskNo;
  }
}
