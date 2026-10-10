package com.guoyu.platform.competition.dto;

public class CompetitionRegistrationAuditRequest {

  private String action;
  private String reason;

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
