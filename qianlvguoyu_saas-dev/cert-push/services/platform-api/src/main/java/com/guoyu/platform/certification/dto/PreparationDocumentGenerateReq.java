package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;

public record PreparationDocumentGenerateReq(
    @NotBlank String docCode
) {
}
