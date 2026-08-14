package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;

public class FollowWorkLogCreateReq {

  @NotBlank
  private String bizDate;
  private Long scheduleId;
  private String authorName;
  private String attendanceSummary;
  private String exceptionNote;
  private String content;
  private String sourceOpIdsJson;
  /** draft / published，默认 published */
  private String status;

  public String getBizDate() {
    return bizDate;
  }

  public void setBizDate(String bizDate) {
    this.bizDate = bizDate;
  }

  public Long getScheduleId() {
    return scheduleId;
  }

  public void setScheduleId(Long scheduleId) {
    this.scheduleId = scheduleId;
  }

  public String getAuthorName() {
    return authorName;
  }

  public void setAuthorName(String authorName) {
    this.authorName = authorName;
  }

  public String getAttendanceSummary() {
    return attendanceSummary;
  }

  public void setAttendanceSummary(String attendanceSummary) {
    this.attendanceSummary = attendanceSummary;
  }

  public String getExceptionNote() {
    return exceptionNote;
  }

  public void setExceptionNote(String exceptionNote) {
    this.exceptionNote = exceptionNote;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getSourceOpIdsJson() {
    return sourceOpIdsJson;
  }

  public void setSourceOpIdsJson(String sourceOpIdsJson) {
    this.sourceOpIdsJson = sourceOpIdsJson;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
