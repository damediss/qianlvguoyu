package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** 创建/更新跟班作业草稿 */
public class FollowTaskSaveReq {

  private Long classId;
  private Long scheduleId;
  private String bizDate;

  @NotBlank
  private String taskType;

  @NotBlank
  @Size(max = 200)
  private String title;

  @Size(max = 1000)
  private String description;

  private String deadlineAt;
  private BigDecimal passScore;
  private String passRule;
  private Boolean showAnalysisAfterSubmit;

  /** 自拟题目；与 sourceExamDetailIds 二选一（可同时有自拟，题库追加） */
  @Valid
  private List<FollowTaskQuestionReq> questions;

  /** 题库选题：exam_exam_detail.id 列表，服务端拷贝为快照 */
  private List<Integer> sourceExamDetailIds;

  public Long getClassId() {
    return classId;
  }

  public void setClassId(Long classId) {
    this.classId = classId;
  }

  public Long getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(Long scheduleId) {
    this.scheduleId = scheduleId;
  }

  public String getBizDate() {
    return bizDate;
  }

  public void setBizDate(String bizDate) {
    this.bizDate = bizDate;
  }

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDeadlineAt() {
    return deadlineAt;
  }

  public void setDeadlineAt(String deadlineAt) {
    this.deadlineAt = deadlineAt;
  }

  public BigDecimal getPassScore() {
    return passScore;
  }

  public void setPassScore(BigDecimal passScore) {
    this.passScore = passScore;
  }

  public String getPassRule() {
    return passRule;
  }

  public void setPassRule(String passRule) {
    this.passRule = passRule;
  }

  public Boolean getShowAnalysisAfterSubmit() {
    return showAnalysisAfterSubmit;
  }

  public void setShowAnalysisAfterSubmit(Boolean showAnalysisAfterSubmit) {
    this.showAnalysisAfterSubmit = showAnalysisAfterSubmit;
  }

  public List<FollowTaskQuestionReq> getQuestions() {
    return questions;
  }

  public void setQuestions(List<FollowTaskQuestionReq> questions) {
    this.questions = questions;
  }

  public List<Integer> getSourceExamDetailIds() {
    return sourceExamDetailIds;
  }

  public void setSourceExamDetailIds(List<Integer> sourceExamDetailIds) {
    this.sourceExamDetailIds = sourceExamDetailIds;
  }
}
