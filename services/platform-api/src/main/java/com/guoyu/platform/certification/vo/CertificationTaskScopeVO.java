package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskScopeEntity;

public record CertificationTaskScopeVO(
    String id,
    String certificationType,
    String workTypeName,
    String workTypeId,
    String levelCode,
    String levelName,
    Integer plannedPeopleCount
) {

  public static CertificationTaskScopeVO from(CertificationTaskScopeEntity entity) {
    return new CertificationTaskScopeVO(
        entity.getId() == null ? null : String.valueOf(entity.getId()),
        entity.getCertificationType(),
        entity.getWorkTypeName(),
        entity.getWorkTypeId() == null ? null : String.valueOf(entity.getWorkTypeId()),
        entity.getLevelCode(),
        entity.getLevelName(),
        entity.getPlannedPeopleCount()
    );
  }
}
