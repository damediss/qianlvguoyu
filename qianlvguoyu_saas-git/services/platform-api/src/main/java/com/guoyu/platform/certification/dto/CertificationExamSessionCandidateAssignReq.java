package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CertificationExamSessionCandidateAssignReq(
    @NotNull List<Long> candidateIds
) {
}
