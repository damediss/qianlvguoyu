package com.guoyu.platform.trainingdepartment.dto;

/** 学习记录创建请求 */
public class TrainUserLearnLogCreateReq {

  /** 培训ID */
  private Long trainId;

  /** 匹配ID */
  private Long matchId;

  /** 培训任务ID */
  private Long trainTaskId;

  /** 学习时长 */
  private Integer learnTime;

  /** 分数/成绩 */
  private Integer score;

  private Long userId;

  private Integer sortNumber;

  public Long getTrainId() {
    return trainId;
  }

  public void setTrainId(Long trainId) {
    this.trainId = trainId;
  }

  public Long getMatchId() {
    return matchId;
  }

  public void setMatchId(Long matchId) {
    this.matchId = matchId;
  }

  public Long getTrainTaskId() {
    return trainTaskId;
  }

  public void setTrainTaskId(Long trainTaskId) {
    this.trainTaskId = trainTaskId;
  }

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

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
