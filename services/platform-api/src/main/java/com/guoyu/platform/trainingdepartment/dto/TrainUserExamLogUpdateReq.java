package com.guoyu.platform.trainingdepartment.dto;

/** 用户刷题记录更新请求 */
public class TrainUserExamLogUpdateReq {

  /** 知识点ID */
  private Long knowledgeId;

  private Integer sortNumber;

  public Long getKnowledgeId() {
    return knowledgeId;
  }

  public void setKnowledgeId(Long knowledgeId) {
    this.knowledgeId = knowledgeId;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
