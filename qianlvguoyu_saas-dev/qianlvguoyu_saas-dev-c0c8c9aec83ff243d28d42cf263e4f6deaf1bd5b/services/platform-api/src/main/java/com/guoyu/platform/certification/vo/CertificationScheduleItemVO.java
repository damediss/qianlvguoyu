package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationScheduleItemEntity;
import java.time.LocalDate;

public record CertificationScheduleItemVO(
    String id,
    Integer dayNo,
    LocalDate planDate,
    LocalDate startDate,
    LocalDate endDate,
    String itemName,
    String itemDescription,
    Boolean required,
    Boolean completed,
    String externalContactMemberId,
    String certificationOwnerMemberId
) {

  public static CertificationScheduleItemVO from(CertificationScheduleItemEntity entity) {
    return new CertificationScheduleItemVO(
        entity.getId() == null ? null : String.valueOf(entity.getId()),
        entity.getDayNo(),
        entity.getPlanDate(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getItemName(),
        entity.getItemDescription(),
        entity.getRequired(),
        entity.getCompleted(),
        entity.getExternalContactMemberId() == null ? null : String.valueOf(entity.getExternalContactMemberId()),
        entity.getCertificationOwnerMemberId() == null ? null : String.valueOf(entity.getCertificationOwnerMemberId())
    );
  }
}
