package com.guoyu.platform.competition.exam.formal.scoring;

public class FormalExamScoringException extends RuntimeException {

  private final Long sessionId;
  private final Long candidateExamId;
  private final Long questionSnapshotId;

  public FormalExamScoringException(String message) {
    this(message, null, null, null, null);
  }

  public FormalExamScoringException(String message, Long sessionId, Long candidateExamId) {
    this(message, sessionId, candidateExamId, null, null);
  }

  public FormalExamScoringException(
      String message, Long sessionId, Long candidateExamId, Long questionSnapshotId) {
    this(message, sessionId, candidateExamId, questionSnapshotId, null);
  }

  public FormalExamScoringException(
      String message,
      Long sessionId,
      Long candidateExamId,
      Long questionSnapshotId,
      Throwable cause) {
    super(message, cause);
    this.sessionId = sessionId;
    this.candidateExamId = candidateExamId;
    this.questionSnapshotId = questionSnapshotId;
  }

  public Long sessionId() {
    return sessionId;
  }

  public Long candidateExamId() {
    return candidateExamId;
  }

  public Long questionSnapshotId() {
    return questionSnapshotId;
  }
}
