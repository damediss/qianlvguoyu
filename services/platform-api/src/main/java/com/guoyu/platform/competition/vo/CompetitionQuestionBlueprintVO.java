package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionQuestionBlueprintVO(
    String id,
    String eventId,
    String eventWorkTypeId,
    String eventWorkTypeLevelId,
    String contentType,
    Integer versionNo,
    String title,
    String sourceFileName,
    String sourceFileId,
    Integer pointCount,
    Integer topicCount,
    String xyzRatio,
    Integer weightCheck,
    String checkReport,
    String status,
    LocalDateTime createdAt
) {
}
