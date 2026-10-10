package com.guoyu.platform.certification.vo;

import java.time.LocalDate;

public record CertificationMcpTaskOverviewVO(
    String taskId,
    String taskNo,
    String taskName,
    String status,
    String currentPhase,
    String currentPhaseName,
    String workTypes,
    String levels,
    LocalDate examDate,
    long candidateCount,
    long assessorCount,
    long collectionCompletedCount,
    long collectionTotalCount,
    double collectionCompletionRate,
    long practicalScoredCount,
    long practicalTotalCount,
    double practicalScoreCompletionRate
) {
}
