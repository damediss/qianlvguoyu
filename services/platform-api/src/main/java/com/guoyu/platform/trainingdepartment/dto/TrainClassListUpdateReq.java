package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 分班更新请求 */
public class TrainClassListUpdateReq {

  /** 是否组长 */
  private Integer isLeader;

  /** 是否班委 */
  private Integer isCommittee;

  /** 班级名称 */
  @Size(max = 255)
  private String className;

  private Integer sortNumber;

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
