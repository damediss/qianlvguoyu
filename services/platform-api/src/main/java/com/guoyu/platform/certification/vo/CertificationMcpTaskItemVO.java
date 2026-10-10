package com.guoyu.platform.certification.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CertificationMcpTaskItemVO(
    String taskId,
    String taskNo,
    String taskName,
    String status,
    String currentPhase,
    String currentPhaseName,
    String workTypes,
    String levels,
    Integer plannedPeopleCount,
    long candidateCount,
    LocalDate planStartDate,
    LocalDate planEndDate,
    LocalDate examDate,
    LocalDateTime lastBusinessUpdatedAt
) {
}
