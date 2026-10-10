package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TrainShiftAssignMaterializeReq {

  @NotNull
  private Long trainInfoId;
  /** algo_class_as_train_class | bind_existing */
  private String mode = "algo_class_as_train_class";
  private Boolean submitForCompanyConfirm = true;
  @Size(max = 80)
  private String classNamePrefix;
  /** 跳过已绑定 train_class_id 的算法班 */
  private Boolean onlyUnbound = true;
  /** 用 train_plan_roster 补全证号/电话 */
  private Boolean matchRoster = true;

  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
  public String getMode() { return mode; }
  public void setMode(String mode) { this.mode = mode; }
  public Boolean getSubmitForCompanyConfirm() { return submitForCompanyConfirm; }
  public void setSubmitForCompanyConfirm(Boolean submitForCompanyConfirm) {
    this.submitForCompanyConfirm = submitForCompanyConfirm;
  }
  public String getClassNamePrefix() { return classNamePrefix; }
  public void setClassNamePrefix(String classNamePrefix) { this.classNamePrefix = classNamePrefix; }
  public Boolean getOnlyUnbound() { return onlyUnbound; }
  public void setOnlyUnbound(Boolean onlyUnbound) { this.onlyUnbound = onlyUnbound; }
  public Boolean getMatchRoster() { return matchRoster; }
  public void setMatchRoster(Boolean matchRoster) { this.matchRoster = matchRoster; }
}
