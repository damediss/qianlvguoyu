package com.guoyu.platform.certification.vo;

import java.time.LocalDateTime;

public record CertificationArchiveTaskVO(
    Long taskId,
    String taskNo,
    String taskName,
    String batchName,
    String workTypeName,
    String levelName,
    LocalDateTime completedAt,
    Integer candidateCount,
    String taskStatus,
    Long archiveId,
    Boolean basicArchived,
    Boolean materialArchived,
    Boolean candidateMaterialArchived,
    Boolean taskMaterialArchived,
    String archiveStatus,
    LocalDateTime archivedAt,
    String lastOperatorName
) {
}
