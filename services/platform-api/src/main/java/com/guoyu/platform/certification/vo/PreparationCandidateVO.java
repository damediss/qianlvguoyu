package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import java.time.LocalDateTime;

public record PreparationCandidateVO(
    Long id,
    Long taskId,
    Long taskScopeId,
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
    LocalDateTime createdAt,
    String admissionTicketNo
) {
  /** Backward-compatible constructor for existing callers that did not expose taskScopeId. */
  public PreparationCandidateVO(
      Long id, Long taskId, String candidateName, String idCardNo, String maskedIdCardNo,
      String phone, String maskedPhone, String levelCode, String levelName, Long workTypeId,
      String workTypeName, String importSource, String orgName, String status,
      Integer documentCompletedCount, Integer documentRequiredCount, Boolean documentComplete,
      Integer documentUploadedCount, Integer documentMissingCount, Integer documentTotalCount,
      LocalDateTime createdAt
  ) {
    this(id, taskId, null, candidateName, idCardNo, maskedIdCardNo, phone, maskedPhone,
        levelCode, levelName, workTypeId, workTypeName, importSource, orgName, status,
        documentCompletedCount, documentRequiredCount, documentComplete, documentUploadedCount,
        documentMissingCount, documentTotalCount, createdAt, null);
  }

  public static PreparationCandidateVO from(CertificationCandidateEntity entity) {
    return new PreparationCandidateVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getTaskScopeId(),
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
        entity.getCreatedAt(),
        entity.getAdmissionTicketNo()
    );
  }

  public PreparationCandidateVO withDocumentSummary(int uploaded, int missing, int total) {
    return new PreparationCandidateVO(
        id,
        taskId,
        taskScopeId,
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
        createdAt,
        admissionTicketNo
    );
  }
}
