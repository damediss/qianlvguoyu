package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;

public record CertificationCandidateExamSessionSaveReq(
    @NotBlank String participationStatus,
    String collectionStatus,
    Long collectionSourceSessionId,
    String exemptReason,
    String remark
) {
}
