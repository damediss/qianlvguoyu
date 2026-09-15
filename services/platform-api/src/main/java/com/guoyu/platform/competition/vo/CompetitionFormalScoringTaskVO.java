package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionFormalScoringTaskVO(
    String id, String examSessionId, String candidateExamId, String contestantMemberId,
    Integer resultRevisionNo,
    String candidateNo, String candidateName,
    String taskStatus, Integer attemptCount, LocalDateTime nextAttemptAt,
    LocalDateTime leaseUntil, String lastError, String requestedReason,
    LocalDateTime startedAt, LocalDateTime completedAt, String candidateStatus) {

  /**
   * Compatibility constructor for callers that do not yet expose the formal candidate status.
   */
  public CompetitionFormalScoringTaskVO(
      String id, String examSessionId, String candidateExamId, String contestantMemberId,
      Integer resultRevisionNo, String candidateNo, String candidateName,
      String taskStatus, Integer attemptCount, LocalDateTime nextAttemptAt,
      LocalDateTime leaseUntil, String lastError, String requestedReason,
      LocalDateTime startedAt, LocalDateTime completedAt) {
    this(id, examSessionId, candidateExamId, contestantMemberId, resultRevisionNo,
        candidateNo, candidateName, taskStatus, attemptCount, nextAttemptAt, leaseUntil,
        lastError, requestedReason, startedAt, completedAt, null);
  }
}
