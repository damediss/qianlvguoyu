package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskMemberEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CertificationMemberVO(
    String id,
    String memberName,
    String roleCode,
    String roleName,
    String departmentName,
    String responsibility,
    LocalDate startDate,
    LocalDate endDate,
    String inputFrequency,
    BigDecimal workloadPercent,
    String maskedPhone,
    String remark
) {

  public static CertificationMemberVO from(CertificationTaskMemberEntity entity) {
    return new CertificationMemberVO(
        entity.getId() == null ? null : String.valueOf(entity.getId()),
        entity.getMemberName(),
        entity.getRoleCode(),
        entity.getRoleName(),
        entity.getDepartmentName(),
        entity.getResponsibility(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getInputFrequency(),
        entity.getWorkloadPercent(),
        entity.getMaskedPhone(),
        entity.getRemark()
    );
  }
}
