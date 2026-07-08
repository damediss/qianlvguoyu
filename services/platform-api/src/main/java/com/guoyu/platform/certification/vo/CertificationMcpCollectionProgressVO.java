package com.guoyu.platform.certification.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CertificationMcpCollectionProgressVO(
    String taskId,
    long totalCount,
    long completedCount,
    long incompleteCount,
    double completionRate,
    List<IncompleteCandidate> incompleteCandidates,
    List<CollectionExceptionItem> exceptionItems
) {

  public record IncompleteCandidate(
      String candidateId,
      String candidateNo,
      String candidateName,
      String workTypeName,
      String levelName,
      String overallStatus,
      String fingerprintStatus,
      String signatureStatus,
      List<String> missingItems
  ) {
  }

  public record CollectionExceptionItem(
      String candidateId,
      String candidateNo,
      String candidateName,
      String exceptionType,
      String exceptionStatus,
      String reason,
      String deviceStatus,
      LocalDateTime handledAt,
      LocalDateTime createdAt
  ) {
  }
}
