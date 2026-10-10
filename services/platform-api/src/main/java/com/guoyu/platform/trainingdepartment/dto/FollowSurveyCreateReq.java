package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class FollowSurveyCreateReq {

  @NotNull
  private BigDecimal score;
  private String respondentName;
  private String commentText;
  private String bizDate;

  public BigDecimal getScore() {
    return score;
  }

  public void setScore(BigDecimal score) {
    this.score = score;
  }

  public String getRespondentName() {
    return respondentName;
  }

  public void setRespondentName(String respondentName) {
    this.respondentName = respondentName;
  }

  public String getCommentText() {
    return commentText;
  }

  public void setCommentText(String commentText) {
    this.commentText = commentText;
  }

  public String getBizDate() {
    return bizDate;
  }

  public void setBizDate(String bizDate) {
    this.bizDate = bizDate;
  }
}
