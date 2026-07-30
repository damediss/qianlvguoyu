package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskPhaseEntity;
import java.time.LocalDateTime;

public record CertificationPhaseVO(
    String id,
    String phaseCode,
    String phaseName,
    Integer phaseOrder,
    String status,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {

  public static CertificationPhaseVO from(CertificationTaskPhaseEntity entity) {
    return new CertificationPhaseVO(
        entity.getId() == null ? null : String.valueOf(entity.getId()),
        entity.getPhaseCode(),
        entity.getPhaseName(),
        entity.getPhaseOrder(),
        entity.getStatus(),
        entity.getStartedAt(),
        entity.getCompletedAt()
    );
  }
}
