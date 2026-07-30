package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** train_sign_up_self 创建请求 */
public class TrainSignUpSelfCreateReq {

  @NotBlank(message = "标题不能为空")
  @Size(max = 255)
  private String title;

  @Size(max = 2000)
  private String cover;

  @NotBlank(message = "培训时间不能为空")
  @Size(max = 255)
  private String trainTime;

  @NotBlank(message = "考试时间不能为空")
  @Size(max = 255)
  private String examTime;

  @NotNull(message = "请选择绑定企业")
  private Long orgId;

  private Long trainInfoId;

  private Integer sortNumber;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCover() {
    return cover;
  }

  public void setCover(String cover) {
    this.cover = cover;
  }

  public String getTrainTime() {
    return trainTime;
  }

  public void setTrainTime(String trainTime) {
    this.trainTime = trainTime;
  }

  public String getExamTime() {
    return examTime;
  }

  public void setExamTime(String examTime) {
    this.examTime = examTime;
  }

  public Long getOrgId() {
    return orgId;
  }

  public void setOrgId(Long orgId) {
    this.orgId = orgId;
  }

  public Long getTrainInfoId() {
    return trainInfoId;
  }

  public void setTrainInfoId(Long trainInfoId) {
    this.trainInfoId = trainInfoId;
  }

  public Integer getSortNumber() {
    return sortNumber;
  }

  public void setSortNumber(Integer sortNumber) {
    this.sortNumber = sortNumber;
  }
}
