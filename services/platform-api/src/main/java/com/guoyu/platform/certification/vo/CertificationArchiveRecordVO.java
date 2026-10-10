package com.guoyu.platform.certification.vo;

import java.time.LocalDateTime;

public record CertificationArchiveRecordVO(
    Long id,
    Long taskId,
    String taskNo,
    String taskName,
    String batchName,
    String workTypeName,
    String levelName,
    LocalDateTime completedAt,
    Integer candidateCount,
    String archiveStatus,
    Boolean basicArchived,
    Boolean materialArchived,
    Long archivedBy,
    LocalDateTime archivedAt,
    String lastOperatorName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
