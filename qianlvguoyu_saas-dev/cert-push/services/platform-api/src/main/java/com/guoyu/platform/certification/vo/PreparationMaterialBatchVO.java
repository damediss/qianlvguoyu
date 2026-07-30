package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationApplicationMaterialBatchEntity;
import java.util.List;

public record PreparationMaterialBatchVO(
    Long id,
    Long taskId,
    String batchNo,
    String batchType,
    String generateScope,
    Integer candidateCount,
    Integer successCount,
    Integer failedCount,
    String status,
    Long packageFileId,
    String packageFileName,
    List<PreparationMaterialItemVO> items
) {
  public static PreparationMaterialBatchVO from(CertificationApplicationMaterialBatchEntity entity) {
    return from(entity, null);
  }

  public static PreparationMaterialBatchVO from(
      CertificationApplicationMaterialBatchEntity entity,
      List<PreparationMaterialItemVO> items
  ) {
    return new PreparationMaterialBatchVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getBatchNo(),
        entity.getBatchType(),
        entity.getGenerateScope(),
        entity.getCandidateCount(),
        entity.getSuccessCount(),
        entity.getFailedCount(),
        entity.getStatus(),
        entity.getPackageFileId(),
        entity.getPackageFileName(),
        items
    );
  }
}
