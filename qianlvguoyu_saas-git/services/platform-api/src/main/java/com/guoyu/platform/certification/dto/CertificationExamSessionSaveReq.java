package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CertificationExamSessionSaveReq(
    Long taskScopeId,
    @NotBlank String examType,
    @NotBlank String sessionName,
    LocalDate examDate,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Long examSiteId,
    String status,
    @NotNull Boolean identityCheckRequired,
    @NotNull Boolean fingerprintRequired,
    @NotNull Boolean signatureRequired,
    String remark
) {
}
