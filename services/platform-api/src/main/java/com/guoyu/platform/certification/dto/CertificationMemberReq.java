package com.guoyu.platform.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CertificationMemberReq(
    Long id,
    Long userId,
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
  public CertificationMemberReq(
      Long id, String memberName, String roleCode, String roleName, String departmentName,
      String responsibility, LocalDate startDate, LocalDate endDate, String inputFrequency,
      BigDecimal workloadPercent, String phone, String remark
  ) {
    this(id, null, memberName, roleCode, roleName, departmentName, responsibility, startDate, endDate,
        inputFrequency, workloadPercent, phone, remark);
  }
}
