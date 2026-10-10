package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CertificationWbsItemReq(
    Long id,
    @NotBlank @Size(max = 60) String wbsCode,
    @Size(max = 60) String categoryCode,
    @Size(max = 80) String categoryName,
    @NotBlank @Size(max = 200) String itemName,
    @Size(max = 1000) String activityDescription,
    BigDecimal estimateHours,
    BigDecimal manpowerCount,
    @Size(max = 60) String manpowerRoleCode,
    Long responsibleMemberId,
    @Size(max = 500) String resourceInput,
    BigDecimal costAmount,
    Integer durationDays,
    Long predecessorId,
    @Size(max = 40) String status
) {
}
