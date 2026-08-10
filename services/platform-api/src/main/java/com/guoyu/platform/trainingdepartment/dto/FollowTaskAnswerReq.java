package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;

/** 单题作答 */
public class FollowTaskAnswerReq {

  @NotNull
  private Long questionId;

  private Object answer;

  public Long getQuestionId() {
    return questionId;
  }

  public void setQuestionId(Long questionId) {
    this.questionId = questionId;
  }

  public Object getAnswer() {
    return answer;
  }

  public void setAnswer(Object answer) {
    this.answer = answer;
  }
}
