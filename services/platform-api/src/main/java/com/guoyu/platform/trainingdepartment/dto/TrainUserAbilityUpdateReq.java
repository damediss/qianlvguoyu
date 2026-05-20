package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 个人技能矩阵更新请求 */
public class TrainUserAbilityUpdateReq {

  /** 评估结果 */
  @Size(max = 20)
  private String result;

  /** 分数 */
  private Integer score;

  /** 当前分数 */
  private Integer currentScore;

  /** 评语 */
  @Size(max = 20)
  private String comments;

  /** 状态 */
  private Integer status;

  /** 排序号 */
  private Integer sortNumber;

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  public Integer getCurrentScore() {
    return currentScore;
  }

  public void setCurrentScore(Integer currentScore) {
    this.currentScore = currentScore;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
