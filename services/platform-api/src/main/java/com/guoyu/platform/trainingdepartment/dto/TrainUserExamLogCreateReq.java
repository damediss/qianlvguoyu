package com.guoyu.platform.trainingdepartment.dto;

/** 用户刷题记录创建请求 */
public class TrainUserExamLogCreateReq {

  /** 培训ID */
  private Long trainId;

  /** 知识点ID */
  private Long knowledgeId;

  private Long userId;

  private Integer sortNumber;

  public Long getTrainId() {
    return trainId;
  }

  public void setTrainId(Long trainId) {
    this.trainId = trainId;
  }

  public Long getKnowledgeId() {
    return knowledgeId;
  }

  public void setKnowledgeId(Long knowledgeId) {
    this.knowledgeId = knowledgeId;
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
