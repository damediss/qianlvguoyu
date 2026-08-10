package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/** 作业题目（自拟客观题） */
public class FollowTaskQuestionReq {

  private Integer sortNumber;

  @NotBlank
  private String questionType;

  @NotBlank
  private String stem;

  private List<Object> options;

  @NotNull
  private Object answer;

  private BigDecimal score;
  private String analysis;
  /** 题库来源 exam_exam_detail.id */
  private Long sourceExamDetailId;

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

  public String getQuestionType() {
    return questionType;
  }

  public void setQuestionType(String questionType) {
    this.questionType = questionType;
  }

  public String getStem() {
    return stem;
  }

  public void setStem(String stem) {
    this.stem = stem;
  }

  public List<Object> getOptions() {
    return options;
  }

  public void setOptions(List<Object> options) {
    this.options = options;
  }

  public Object getAnswer() {
    return answer;
  }

  public void setAnswer(Object answer) {
    this.answer = answer;
  }

  public BigDecimal getScore() {
    return score;
  }

  public void setScore(BigDecimal score) {
    this.score = score;
  }

  public String getAnalysis() {
    return analysis;
  }

  public void setAnalysis(String analysis) {
    this.analysis = analysis;
  }

  public Long getSourceExamDetailId() {
    return sourceExamDetailId;
  }

  public void setSourceExamDetailId(Long sourceExamDetailId) {
    this.sourceExamDetailId = sourceExamDetailId;
  }
}
