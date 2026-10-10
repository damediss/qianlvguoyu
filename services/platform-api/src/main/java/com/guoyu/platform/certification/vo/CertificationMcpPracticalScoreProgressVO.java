package com.guoyu.platform.certification.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CertificationMcpPracticalScoreProgressVO(
    String taskId,
    long totalCount,
    long scoredCount,
    long unscoredCount,
    double completionRate,
    List<UnscoredCandidate> unscoredCandidates,
    List<AnomalyItem> anomalyItems
) {

  public record UnscoredCandidate(
      String candidateId,
      String candidateNo,
      String candidateName,
      String workTypeName,
      String levelName,
      String resultStatus,
      int completedQuestionCount,
      int questionCount
  ) {
  }

  public record AnomalyItem(
      String candidateId,
      String candidateNo,
      String candidateName,
      String anomalyType,
      String status,
      String description,
      String assessorTaskId,
      String questionTitle,
      BigDecimal scoreDeviation,
      LocalDateTime occurredAt
  ) {
  }
}
