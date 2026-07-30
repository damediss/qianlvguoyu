package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.Size;

/** train_sign_up_self 更新请求 */
public class TrainSignUpSelfUpdateReq {

  @Size(max = 255)
  /** 标题 */
  private String title;

  @Size(max = 255)
  /** 封面图 */
  private String cover;

  @Size(max = 255)
  /** 培训时间 */
  private String trainTime;

  @Size(max = 255)
  /** 考试时间 */
  private String examTime;

  @Size(max = 255)
  /** 组织ID */
  private Long orgId;

  @Size(max = 255)
  /** 培训信息ID */
  private Long trainInfoId;

  @Size(max = 255)
  /** 排序号 */
  private Integer sortNumber;
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getCover() { return cover; }
  public void setCover(String cover) { this.cover = cover; }
  public String getTrainTime() { return trainTime; }
  public void setTrainTime(String trainTime) { this.trainTime = trainTime; }
  public String getExamTime() { return examTime; }
  public void setExamTime(String examTime) { this.examTime = examTime; }
  public Long getOrgId() { return orgId; }
  public void setOrgId(Long orgId) { this.orgId = orgId; }
  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
  public Integer getSortNumber() { return sortNumber; }
  public void setSortNumber(Integer sortNumber) { this.sortNumber = sortNumber; }
}
