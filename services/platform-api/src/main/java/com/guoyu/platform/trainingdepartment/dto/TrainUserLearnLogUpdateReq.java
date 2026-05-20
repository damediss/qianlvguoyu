package com.guoyu.platform.trainingdepartment.dto;

/** 学习记录更新请求 */
public class TrainUserLearnLogUpdateReq {

  /** 学习时长 */
  private Integer learnTime;

  /** 分数/成绩 */
  private Integer score;

  /** 排序号 */
  private Integer sortNumber;

  public Integer getLearnTime() {
    return learnTime;
  }

  public void setLearnTime(Integer learnTime) {
    this.learnTime = learnTime;
  }

  public Integer getScore() {
    return score;
  }

  public void setScore(Integer score) {
    this.score = score;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
