package com.guoyu.platform.competition.enums;

public enum CompetitionPlayerStatus {
  PENDING_ACTIVATION("pending_activation"),
  ACTIVE("active"),
  DISABLED("disabled");

  private final String code;

  CompetitionPlayerStatus(String code) { this.code = code; }
  public String getCode() { return code; }

  /**
   * On-site exam tablets authenticate with the imported mobile + initial password.
   * SMS activation is only required for the player H5 personal center, not for sitting an exam,
   * especially on an isolated hotspot with no SMS path.
   */
  public static boolean allowsExamAccess(String status) {
    return PENDING_ACTIVATION.code.equals(status) || ACTIVE.code.equals(status);
  }
}
