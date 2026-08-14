package com.guoyu.platform.trainingdepartment.dto;

public class FollowIssueTransitionReq {

  /** open / doing / done / closed */
  private String status;
  private String resolveNote;
  private String assigneeName;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getResolveNote() {
    return resolveNote;
  }

  public void setResolveNote(String resolveNote) {
    this.resolveNote = resolveNote;
  }

  public String getAssigneeName() {
    return assigneeName;
  }

  public void setAssigneeName(String assigneeName) {
    this.assigneeName = assigneeName;
  }
}
