package com.guoyu.platform.trainingdepartment.dto;

import java.math.BigDecimal;

/** 考试结果更新请求 */
public class ExamResultUpdateReq {

  /** 题型 */
  private String type;

  /** 获得分数 */
  private BigDecimal getScore;

  /** 0正考 1模拟考 2能力测评 3顺序练习 4随机练习 */
  private Integer examType;

  private String userAnswer;

  /** 排序号 */
  private Integer sortNumber;

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public BigDecimal getGetScore() { return getScore; }
  public void setGetScore(BigDecimal getScore) { this.getScore = getScore; }
  public Integer getExamType() { return examType; }
  public void setExamType(Integer examType) { this.examType = examType; }
  public String getUserAnswer() { return userAnswer; }
  public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
