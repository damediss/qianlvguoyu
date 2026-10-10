package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 个人技能矩阵创建请求 */
public class TrainUserAbilityCreateReq {

  /** 培训ID */
  private Long trainId;

  /** 课程ID */
  private Long courseId;

  /** 知识点ID */
  private Long knowledgeId;

  /** 评估结果 */
  @Size(max = 20)
  private String result;

  /** 配置ID */
  private Long configId;

  /** 分数 */
  private Integer score;

  /** 当前分数 */
  private Integer currentScore;

  /** 用户ID */
  private Long userId;

  /** 平台用户ID */
  private Long platformUserId;

  /** 评语 */
  @Size(max = 20)
  private String comments;

  /** 状态 */
  private Integer status;

  /** 排序号 */
  private Integer sortNumber;

  public Long getTrainId() {
    return trainId;
  }

  public void setTrainId(Long trainId) {
    this.trainId = trainId;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getKnowledgeId() {
    return knowledgeId;
  }

  public void setKnowledgeId(Long knowledgeId) {
    this.knowledgeId = knowledgeId;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public Long getConfigId() {
    return configId;
  }

  public void setConfigId(Long configId) {
    this.configId = configId;
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

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getPlatformUserId() {
    return platformUserId;
  }

  public void setPlatformUserId(Long platformUserId) {
    this.platformUserId = platformUserId;
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
