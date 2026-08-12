package com.guoyu.platform.trainingdepartment.dto;

import com.guoyu.platform.exam.dto.ExamMakeExamPlanCreateReq;
import java.math.BigDecimal;

/** 跟班模拟考：调用组卷计划添加组卷并挂接本班 mock */
public class FollowMockWithPlanReq {

  /** 组卷计划创建参数（与题库添加组卷一致） */
  private ExamMakeExamPlanCreateReq plan;
  private String title;
  private String description;
  private String bizDate;
  private BigDecimal passScore;
  private Boolean showAnalysisAfterSubmit;
  private Boolean excludeOtherMock = true;

  public ExamMakeExamPlanCreateReq getPlan() {
    return plan;
  }

  public void setPlan(ExamMakeExamPlanCreateReq plan) {
    this.plan = plan;
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
