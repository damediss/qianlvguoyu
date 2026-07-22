package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** 手动创建分班草稿（strategy=manual），不立即建班。 */
public class TrainDivisionManualPackReq {

  @NotNull
  private Long planId;
  @NotNull
  private Long trainInfoId;
  @Min(1)
  @Max(200)
  private Integer classMax = 50;
  @Size(max = 80)
  private String classNamePrefix;
  @Valid
  private List<ClassSeed> classes = new ArrayList<>();

  public Long getPlanId() { return planId; }
  public void setPlanId(Long planId) { this.planId = planId; }
  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
  public Integer getClassMax() { return classMax; }
  public void setClassMax(Integer classMax) { this.classMax = classMax; }
  public String getClassNamePrefix() { return classNamePrefix; }
  public void setClassNamePrefix(String classNamePrefix) { this.classNamePrefix = classNamePrefix; }
  public List<ClassSeed> getClasses() { return classes; }
  public void setClasses(List<ClassSeed> classes) { this.classes = classes; }

  public static class ClassSeed {
    @Size(max = 100)
    private String title;
    private List<Long> rosterIds = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<Long> getRosterIds() { return rosterIds; }
    public void setRosterIds(List<Long> rosterIds) { this.rosterIds = rosterIds; }
  }
}
