package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class TrainDivisionOrgSplitReq {

  @NotNull
  private Long planId;
  @NotNull
  private Long trainInfoId;
  @NotEmpty
  @Valid
  private List<ClassItem> classes;
  private Boolean submitForCompanyConfirm = true;

  public Long getPlanId() { return planId; }
  public void setPlanId(Long planId) { this.planId = planId; }
  public Long getTrainInfoId() { return trainInfoId; }
  public void setTrainInfoId(Long trainInfoId) { this.trainInfoId = trainInfoId; }
  public List<ClassItem> getClasses() { return classes; }
  public void setClasses(List<ClassItem> classes) { this.classes = classes; }
  public Boolean getSubmitForCompanyConfirm() { return submitForCompanyConfirm; }
  public void setSubmitForCompanyConfirm(Boolean submitForCompanyConfirm) {
    this.submitForCompanyConfirm = submitForCompanyConfirm;
  }

  public static class ClassItem {
    private Long classId;
    @NotBlank
    @Size(max = 100)
    private String title;
    private Long workTypeId;
    private Integer workTypeLevel;
    private String dateStart;
    private String dateEnd;
    @NotEmpty
    private List<Long> rosterIds;

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getWorkTypeId() { return workTypeId; }
    public void setWorkTypeId(Long workTypeId) { this.workTypeId = workTypeId; }
    public Integer getWorkTypeLevel() { return workTypeLevel; }
    public void setWorkTypeLevel(Integer workTypeLevel) { this.workTypeLevel = workTypeLevel; }
    public String getDateStart() { return dateStart; }
    public void setDateStart(String dateStart) { this.dateStart = dateStart; }
    public String getDateEnd() { return dateEnd; }
    public void setDateEnd(String dateEnd) { this.dateEnd = dateEnd; }
    public List<Long> getRosterIds() { return rosterIds; }
    public void setRosterIds(List<Long> rosterIds) { this.rosterIds = rosterIds; }
  }
}
