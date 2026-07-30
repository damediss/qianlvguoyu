package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationCandidateImportErrorEntity;

public record PreparationImportErrorVO(
    Long id,
    Long batchId,
    Long taskId,
    Integer rowNo,
    String candidateName,
    String idCardNo,
    String phone,
    String errorCode,
    String errorMessage
) {
  public static PreparationImportErrorVO from(CertificationCandidateImportErrorEntity entity) {
    return new PreparationImportErrorVO(
        entity.getId(),
        entity.getBatchId(),
        entity.getTaskId(),
        entity.getRowNo(),
        entity.getCandidateName(),
        entity.getIdCardNo(),
        entity.getPhone(),
        entity.getErrorCode(),
        entity.getErrorMessage()
    );
  }
}
