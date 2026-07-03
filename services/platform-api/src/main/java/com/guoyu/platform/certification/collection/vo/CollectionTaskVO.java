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
    Integer exceptionCount
) {
}
