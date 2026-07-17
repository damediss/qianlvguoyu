package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TrainShiftAssignBindClassReq {

  @NotBlank
  private String algoClassName;
  @NotNull
  private Long trainClassId;

  public String getAlgoClassName() { return algoClassName; }
  public void setAlgoClassName(String algoClassName) { this.algoClassName = algoClassName; }
  public Long getTrainClassId() { return trainClassId; }
  public void setTrainClassId(Long trainClassId) { this.trainClassId = trainClassId; }
}
