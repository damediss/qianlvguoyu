package com.guoyu.platform.certification.collection.vo;

import java.time.LocalDate;

public record CollectionTaskVO(
    Long taskId,
    String taskNo,
    String taskName,
    String projectName,
    String workType,
    String level,
    LocalDate examDate,
    Integer candidateTotal,
    Integer qrIssuedCount,
    Integer arrivedCount,
    Integer queuedCount,
    Integer collectingCount,
    Integer completedCount,
    Integer pendingRetryCount,
    Integer exceptionCount,
    Long examSessionId,
    String sessionName,
    String examType,
    String workTypeLevel
) {
  public CollectionTaskVO(
      Long taskId,
      String taskNo,
      String taskName,
      String projectName,
      String workType,
      String level,
      LocalDate examDate,
      Integer candidateTotal,
      Integer qrIssuedCount,
      Integer arrivedCount,
      Integer queuedCount,
      Integer collectingCount,
      Integer completedCount,
      Integer pendingRetryCount,
      Integer exceptionCount
  ) {
    this(taskId, taskNo, taskName, projectName, workType, level, examDate, candidateTotal,
        qrIssuedCount, arrivedCount, queuedCount, collectingCount, completedCount,
        pendingRetryCount, exceptionCount, null, null, null, null);
  }
}
