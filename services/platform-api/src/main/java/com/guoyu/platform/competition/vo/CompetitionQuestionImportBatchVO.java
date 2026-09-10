package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionQuestionImportBatchVO(
    String id,
    String eventId,
    String eventWorkTypeId,
    String eventWorkTypeLevelId,
    String contentType,
    String originalFilename,
    String status,
    Integer totalCount,
    Integer successCount,
    Integer failureCount,
    String errorDetail,
    LocalDateTime startedAt,
    LocalDateTime finishedAt
) {
}
