package com.guoyu.platform.certification.vo;

import java.time.LocalDateTime;

public record CertificationArchiveImportBatchVO(
    Long id,
    Long archiveId,
    String batchNo,
    String importType,
    String zipFileName,
    String status,
    Integer totalCandidates,
    Integer successCount,
    Integer errorCount,
    String statisticsJson,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
