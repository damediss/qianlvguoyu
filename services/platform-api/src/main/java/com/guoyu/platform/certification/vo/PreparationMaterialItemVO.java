package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationApplicationMaterialItemEntity;

public record PreparationMaterialItemVO(
    Long id,
    Long taskId,
    Long batchId,
    Long candidateId,
    String itemCode,
    String itemName,
    Boolean required,
    Long fileId,
    String fileName,
    Integer versionNo,
    String status
) {
  public static PreparationMaterialItemVO from(CertificationApplicationMaterialItemEntity entity) {
    return new PreparationMaterialItemVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getBatchId(),
        entity.getCandidateId(),
        entity.getItemCode(),
        entity.getItemName(),
        entity.getRequired(),
        entity.getFileId(),
        entity.getFileName(),
        entity.getVersionNo(),
        entity.getStatus()
    );
  }
}
