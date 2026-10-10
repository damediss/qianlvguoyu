package com.guoyu.platform.competition.exam.realtime;

import java.time.Instant;
import org.springframework.util.StringUtils;

public final class FormalExamConnectionStates {

  public static final String ONLINE = "online";
  public static final String BRIEFLY_OFFLINE = "briefly_offline";
  public static final String KICKED = "kicked";
  public static final String TOKEN_EXPIRED = "token_expired";
  public static final String SUBMITTED = "submitted";
  public static final String ABSENT = "absent";

  private FormalExamConnectionStates() {
  }

  public static String resolve(
      String candidateStatus,
      String accessSessionStatus,
      Instant lastSeenAt,
      Instant now,
      int onlineWithinSeconds) {
    if ("absent".equals(candidateStatus)) {
      return ABSENT;
    }
    if ("submitted".equals(candidateStatus)
        || "auto_submitted".equals(candidateStatus)
        || "forced_submitted".equals(candidateStatus)) {
      return SUBMITTED;
    }
    if ("kicked".equals(accessSessionStatus)) {
      return KICKED;
    }
    if ("expired".equals(accessSessionStatus)) {
      return TOKEN_EXPIRED;
    }
    int window = Math.max(20, onlineWithinSeconds);
    if (lastSeenAt != null && now != null && !lastSeenAt.isBefore(now.minusSeconds(window))) {
      return ONLINE;
    }
    return BRIEFLY_OFFLINE;
  }

  public static boolean isOnline(String connectionState) {
    return ONLINE.equals(connectionState);
  }

  public static String safeStatus(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
