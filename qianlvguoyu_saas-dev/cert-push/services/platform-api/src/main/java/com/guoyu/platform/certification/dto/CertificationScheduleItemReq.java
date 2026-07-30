package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CertificationScheduleItemReq(
    Long id,
    Integer dayNo,
    LocalDate planDate,
    LocalDate startDate,
    LocalDate endDate,
    @NotBlank @Size(max = 200) String itemName,
    @Size(max = 1000) String itemDescription,
    Boolean required,
    Boolean completed,
    Long externalContactMemberId,
    Long certificationOwnerMemberId
) {
}
