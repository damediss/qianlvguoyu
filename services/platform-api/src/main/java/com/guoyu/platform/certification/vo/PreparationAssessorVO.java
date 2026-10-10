package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationAssessorEntity;
import java.time.LocalDate;

public record PreparationAssessorVO(
    Long id,
    Long userId,
    String assessorName,
    String certificateNo,
    String workTypeScope,
    String levelScope,
    LocalDate validUntil,
    String phone,
    String status,
    String remark
) {
  public static PreparationAssessorVO from(CertificationAssessorEntity entity) {
    return new PreparationAssessorVO(
        entity.getId(),
        entity.getUserId(),
        entity.getAssessorName(),
        entity.getCertificateNo(),
        entity.getWorkTypeScope(),
        entity.getLevelScope(),
        entity.getValidUntil(),
        entity.getPhone(),
        entity.getStatus(),
        entity.getRemark()
    );
  }
}
