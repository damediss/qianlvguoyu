package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationTaskDocumentEntity;
import java.time.LocalDateTime;

public record CertificationDocumentVO(
    String id,
    String documentType,
    String documentName,
    Integer versionNo,
    String status,
    Integer completionPercent,
    String contentText,
    String contentJson,
    LocalDateTime submittedAt,
    LocalDateTime approvedAt,
    LocalDateTime rejectedAt,
    String rejectReason,
    String processInstanceId,
    LocalDateTime lastSavedAt
) {

  public static CertificationDocumentVO from(CertificationTaskDocumentEntity entity) {
    return new CertificationDocumentVO(
        entity.getId() == null ? null : String.valueOf(entity.getId()),
        entity.getDocumentType(),
        entity.getDocumentName(),
        entity.getVersionNo(),
        entity.getStatus(),
        entity.getCompletionPercent(),
        entity.getContentText(),
        entity.getContentJson(),
        entity.getSubmittedAt(),
        entity.getApprovedAt(),
        entity.getRejectedAt(),
        entity.getRejectReason(),
        entity.getProcessInstanceId(),
        entity.getLastSavedAt()
    );
  }
}
