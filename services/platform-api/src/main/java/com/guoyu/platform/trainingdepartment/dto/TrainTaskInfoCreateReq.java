package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 培训配置任务列表创建请求 */
public class TrainTaskInfoCreateReq {

  /** 培训ID */
  private Long trainId;

  /** 封面 */
  @Size(max = 255)
  private String cover;

  /** 课程ID */
  private Long courseId;

  /** 培训任务ID */
  private Long trainTaskId;

  /** 知识点ID */
  private Long knowledgeId;

  /** 简介 */
  private String intro;

  /** 是否必修 */
  private Integer isRequired;

  /** 分数 */
  private Integer score;

  /** 用户ID */
  private Long userId;

  /** 排序号 */
  private Integer sortNumber;

  public Long getTrainId() {
    return trainId;
  }

  public void setTrainId(Long trainId) {
    this.trainId = trainId;
  }

  public String getCover() {
    return cover;
  }

  public void setCover(String cover) {
    this.cover = cover;
  }

  public Long getCourseId() {
    return courseId;
  }

  public void setCourseId(Long courseId) {
    this.courseId = courseId;
  }

  public Long getTrainTaskId() {
    return trainTaskId;
  }

  public void setTrainTaskId(Long trainTaskId) {
    this.trainTaskId = trainTaskId;
  }

  public Long getKnowledgeId() {
    return knowledgeId;
  }

  public void setKnowledgeId(Long knowledgeId) {
    this.knowledgeId = knowledgeId;
  }

  public String getIntro() {
    return intro;
  }

  public void setIntro(String intro) {
    this.intro = intro;
  }

  public Integer getIsRequired() {
    return isRequired;
  }

  public void setIsRequired(Integer isRequired) {
    this.isRequired = isRequired;
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
