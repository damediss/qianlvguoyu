package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotNull;

public record CertificationDocumentReviewReq(
    @NotNull Boolean approved,
    String comment
) {
}
