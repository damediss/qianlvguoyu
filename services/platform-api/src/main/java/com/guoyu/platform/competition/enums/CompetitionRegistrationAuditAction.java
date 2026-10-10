package com.guoyu.platform.competition.enums;

public enum CompetitionRegistrationAuditAction {
  APPROVE("approve", CompetitionRegistrationStatus.ELIGIBLE_UNACTIVATED),
  SUPPLEMENT("supplement", CompetitionRegistrationStatus.SUPPLEMENT_REQUIRED),
  REJECT("reject", CompetitionRegistrationStatus.REJECTED);

  private final String code;
  private final CompetitionRegistrationStatus targetStatus;

  CompetitionRegistrationAuditAction(String code, CompetitionRegistrationStatus targetStatus) {
    this.code = code;
    this.targetStatus = targetStatus;
  }

  public String getCode() {
    return code;
  }

  public CompetitionRegistrationStatus getTargetStatus() {
    return targetStatus;
  }

  public static CompetitionRegistrationAuditAction fromCode(String code) {
    if (code == null) {
      return null;
    }
    for (CompetitionRegistrationAuditAction action : values()) {
      if (action.code.equals(code)) {
        return action;
      }
    }
    return null;
  }
}
