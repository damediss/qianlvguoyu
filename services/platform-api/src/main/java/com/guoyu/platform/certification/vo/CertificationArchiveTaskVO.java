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
    String archiveStatus,
    LocalDateTime archivedAt,
    String lastOperatorName
) {
}
