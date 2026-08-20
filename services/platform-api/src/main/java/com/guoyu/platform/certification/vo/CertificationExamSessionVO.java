package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationExamSessionEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CertificationExamSessionVO(
    Long id,
    Long taskId,
    Long taskScopeId,
    String examType,
    String sessionName,
    LocalDate examDate,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Long examSiteId,
    String examVenue,
    String status,
    Boolean identityCheckRequired,
    Boolean fingerprintRequired,
    Boolean signatureRequired,
    String remark
) {
  public static CertificationExamSessionVO from(CertificationExamSessionEntity entity) {
    return new CertificationExamSessionVO(entity.getId(), entity.getTaskId(), entity.getTaskScopeId(),
        entity.getExamType(), entity.getSessionName(), entity.getExamDate(), entity.getStartTime(),
        entity.getEndTime(), entity.getExamSiteId(), entity.getExamVenue(), entity.getStatus(),
        entity.getIdentityCheckRequired(), entity.getFingerprintRequired(), entity.getSignatureRequired(),
        entity.getRemark());
  }
}
