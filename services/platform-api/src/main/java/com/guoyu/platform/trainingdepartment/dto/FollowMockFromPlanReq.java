package com.guoyu.platform.trainingdepartment.dto;

import java.math.BigDecimal;

/** 跟班模拟考：选用已有组卷计划挂接本班 mock */
public class FollowMockFromPlanReq {

  private Integer makeExamPlanId;
  private String title;
  private String description;
  private String bizDate;
  private BigDecimal passScore;
  private Boolean showAnalysisAfterSubmit;
  private Boolean excludeOtherMock = true;

  public Integer getMakeExamPlanId() {
    return makeExamPlanId;
  }

  public void setMakeExamPlanId(Integer makeExamPlanId) {
    this.makeExamPlanId = makeExamPlanId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getBizDate() {
    return bizDate;
  }

  public void setBizDate(String bizDate) {
    this.bizDate = bizDate;
  }

  public BigDecimal getPassScore() {
    return passScore;
  }

  public void setPassScore(BigDecimal passScore) {
    this.passScore = passScore;
  }

  public Boolean getShowAnalysisAfterSubmit() {
    return showAnalysisAfterSubmit;
  }

  public void setShowAnalysisAfterSubmit(Boolean showAnalysisAfterSubmit) {
    this.showAnalysisAfterSubmit = showAnalysisAfterSubmit;
  }

  public Boolean getExcludeOtherMock() {
    return excludeOtherMock;
  }

  public void setExcludeOtherMock(Boolean excludeOtherMock) {
    this.excludeOtherMock = excludeOtherMock;
  }
}
