package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;

public record PreparationDocumentReviewReq(
    @NotBlank String status,
    String reviewComment
) {
}
