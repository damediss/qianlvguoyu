package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CertificationRiskStatusReq(
    @NotBlank @Size(max = 40) String status,
    @Size(max = 500) String closeReason
) {
}
