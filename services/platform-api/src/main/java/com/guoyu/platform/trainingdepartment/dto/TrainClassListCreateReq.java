package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 分班创建请求 */
public class TrainClassListCreateReq {

  /** 培训ID */
  private Long trainId;

  /** 用户ID */
  private Long userId;

  /** 是否组长 */
  private Integer isLeader;

  /** 是否班委 */
  private Integer isCommittee;

  /** 班级名称 */
  @Size(max = 255)
  /** 班级名称 */
  private String className;

  /** 排序号 */
  private Integer sortNumber;

  public Long getTrainId() {
    return trainId;
  }

  public void setTrainId(Long trainId) {
    this.trainId = trainId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Integer getIsLeader() {
    return isLeader;
  }

  public void setIsLeader(Integer isLeader) {
    this.isLeader = isLeader;
  }

  public Integer getIsCommittee() {
    return isCommittee;
  }

  public void setIsCommittee(Integer isCommittee) {
    this.isCommittee = isCommittee;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
