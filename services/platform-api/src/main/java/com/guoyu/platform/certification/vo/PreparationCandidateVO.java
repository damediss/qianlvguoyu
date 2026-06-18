package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import java.time.LocalDateTime;

public record PreparationCandidateVO(
    Long id,
    Long taskId,
    String candidateName,
    String idCardNo,
    String maskedIdCardNo,
    String phone,
    String maskedPhone,
    String levelCode,
    String levelName,
    Long workTypeId,
    String workTypeName,
    String importSource,
    String orgName,
    String status,
    Integer documentCompletedCount,
    Integer documentRequiredCount,
    Boolean documentComplete,
    Integer documentUploadedCount,
    Integer documentMissingCount,
    Integer documentTotalCount,
    LocalDateTime createdAt
) {
  public static PreparationCandidateVO from(CertificationCandidateEntity entity) {
    return new PreparationCandidateVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getCandidateName(),
        entity.getIdCardNo(),
        entity.getMaskedIdCardNo(),
        entity.getPhone(),
        entity.getMaskedPhone(),
        entity.getLevelCode(),
        entity.getLevelName(),
        entity.getWorkTypeId(),
        entity.getWorkTypeName(),
        entity.getImportSource(),
        entity.getOrgName(),
        entity.getStatus(),
        entity.getDocumentCompletedCount(),
        entity.getDocumentRequiredCount(),
        entity.getDocumentComplete(),
        null,
        null,
        null,
        entity.getCreatedAt()
    );
  }

  public PreparationCandidateVO withDocumentSummary(int uploaded, int missing, int total) {
    return new PreparationCandidateVO(
        id,
        taskId,
        candidateName,
        idCardNo,
        maskedIdCardNo,
        phone,
        maskedPhone,
        levelCode,
        levelName,
        workTypeId,
        workTypeName,
        importSource,
        orgName,
        status,
        documentCompletedCount,
        documentRequiredCount,
        documentComplete,
        uploaded,
        missing,
        total,
        createdAt
    );
  }
}
