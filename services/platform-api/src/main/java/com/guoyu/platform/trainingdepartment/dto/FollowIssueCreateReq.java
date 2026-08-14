package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotBlank;

public class FollowIssueCreateReq {

  @NotBlank
  private String title;
  private String description;
  /** info / warn / critical */
  private String severity;
  private String assigneeName;
  private String dueDate;
  private Long supervisionId;

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

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getAssigneeName() {
    return assigneeName;
  }

  public void setAssigneeName(String assigneeName) {
    this.assigneeName = assigneeName;
  }

  public String getDueDate() {
    return dueDate;
  }

  public void setDueDate(String dueDate) {
    this.dueDate = dueDate;
  }

  public Long getSupervisionId() {
    return supervisionId;
  }

  public void setSupervisionId(Long supervisionId) {
    this.supervisionId = supervisionId;
  }
}
