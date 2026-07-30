package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CertificationRiskReq(
    Long id,
    @NotBlank @Size(max = 1000) String riskDescription,
    @NotBlank @Size(max = 20) String probabilityCode,
    @NotBlank @Size(max = 20) String impactCode,
    @Size(max = 1000) String responsePlan,
    Long ownerMemberId,
    Long ownerUserId,
    @Size(max = 80) String ownerName,
    LocalDate expectedCloseDate,
    @Size(max = 500) String remark
) {
}
