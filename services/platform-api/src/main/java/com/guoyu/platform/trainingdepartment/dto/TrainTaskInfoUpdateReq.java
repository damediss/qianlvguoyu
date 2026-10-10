package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** 培训配置任务列表更新请求 */
public class TrainTaskInfoUpdateReq {

  /** 封面 */
  @Size(max = 255)
  private String cover;

  /** 简介 */
  @Size(max = 255)
  private String intro;

  /** 是否必修 */
  private Integer isRequired;

  /** 分数 */
  private Integer score;

  /** 排序号 */
  private Integer sortNumber;

  public String getCover() {
    return cover;
  }

  public void setCover(String cover) {
    this.cover = cover;
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

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }

}
