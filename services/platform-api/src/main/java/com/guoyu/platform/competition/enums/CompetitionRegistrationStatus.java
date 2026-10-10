package com.guoyu.platform.competition.enums;

public enum CompetitionRegistrationStatus {
  IMPORTED_PENDING_CONFIRM("imported_pending_confirm"),
  PENDING_AUDIT("pending_audit"),
  SUPPLEMENT_REQUIRED("supplement_required"),
  REJECTED("rejected"),
  ELIGIBLE_UNACTIVATED("eligible_unactivated"),
  ELIGIBLE_ACTIVATED("eligible_activated"),
  WITHDRAWN("withdrawn"),
  EXPIRED("expired");

  private final String code;

  CompetitionRegistrationStatus(String code) { this.code = code; }
  public String getCode() { return code; }
}
