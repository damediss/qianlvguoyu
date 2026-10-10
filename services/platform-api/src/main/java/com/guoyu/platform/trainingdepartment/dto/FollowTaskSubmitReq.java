package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 学员交卷 */
public class FollowTaskSubmitReq {

  @NotEmpty
  @Valid
  private List<FollowTaskAnswerReq> answers;

  public List<FollowTaskAnswerReq> getAnswers() {
    return answers;
  }

  public void setAnswers(List<FollowTaskAnswerReq> answers) {
    this.answers = answers;
  }
}
