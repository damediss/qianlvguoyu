package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationCandidateDocumentEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import java.time.LocalDateTime;

public record PreparationCandidateDocumentVO(
    Long id,
    Long taskId,
    Long candidateId,
    String candidateName,
    String maskedIdCardNo,
    String docCode,
    String docName,
    Boolean required,
    Long fileId,
    String fileName,
    String fileType,
    Integer versionNo,
    String status,
    LocalDateTime uploadedAt,
    LocalDateTime reviewedAt,
    String reviewComment
) {
  public static PreparationCandidateDocumentVO from(CertificationCandidateDocumentEntity entity) {
    return from(entity, null);
  }

  public static PreparationCandidateDocumentVO from(
      CertificationCandidateDocumentEntity entity,
      CertificationCandidateEntity candidate
  ) {
    return new PreparationCandidateDocumentVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getCandidateId(),
        candidate == null ? null : candidate.getCandidateName(),
        candidate == null ? null : candidate.getMaskedIdCardNo(),
        entity.getDocCode(),
        entity.getDocName(),
        entity.getRequired(),
        entity.getFileId(),
        entity.getFileName(),
        entity.getFileType(),
        entity.getVersionNo(),
        entity.getStatus(),
        entity.getUploadedAt(),
        entity.getReviewedAt(),
        entity.getReviewComment()
    );
  }
}
