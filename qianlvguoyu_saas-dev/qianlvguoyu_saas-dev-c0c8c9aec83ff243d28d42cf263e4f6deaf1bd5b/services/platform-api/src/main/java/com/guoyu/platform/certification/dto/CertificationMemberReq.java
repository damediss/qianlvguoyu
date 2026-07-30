package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CertificationMemberReq(
    Long id,
    @NotBlank @Size(max = 80) String memberName,
    @Size(max = 60) String roleCode,
    @Size(max = 80) String roleName,
    @Size(max = 120) String departmentName,
    @Size(max = 500) String responsibility,
    LocalDate startDate,
    LocalDate endDate,
    @Size(max = 40) String inputFrequency,
    BigDecimal workloadPercent,
    @Size(max = 30) String phone,
    @Size(max = 500) String remark
) {
}
