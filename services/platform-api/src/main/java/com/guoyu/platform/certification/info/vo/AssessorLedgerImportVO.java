package com.guoyu.platform.certification.info.vo;

import java.time.LocalDateTime;

public record AssessorLedgerImportVO(
    Long id,
    String status,
    Integer totalCount,
    Integer importedCount,
    Integer skippedCount,
    Integer failedCount,
    String resultMessage,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {
}
