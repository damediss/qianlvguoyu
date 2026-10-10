package com.guoyu.platform.certification.vo;

import com.guoyu.platform.certification.entity.CertificationCandidateImportBatchEntity;
import java.time.LocalDateTime;

public record PreparationImportBatchVO(
    Long id,
    Long taskId,
    Long taskScopeId,
    String batchNo,
    String sourceType,
    Integer totalCount,
    Integer successCount,
    Integer failedCount,
    Integer duplicateCount,
    String status,
    LocalDateTime completedAt
) {
  public static PreparationImportBatchVO from(CertificationCandidateImportBatchEntity entity) {
    return new PreparationImportBatchVO(
        entity.getId(),
        entity.getTaskId(),
        entity.getTaskScopeId(),
        entity.getBatchNo(),
        entity.getSourceType(),
        entity.getTotalCount(),
        entity.getSuccessCount(),
        entity.getFailedCount(),
        entity.getDuplicateCount(),
        entity.getStatus(),
        entity.getCompletedAt()
    );
  }
}
