package com.guoyu.platform.competition.prepractice.vo;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record CompetitionPrePracticeTaskVO(
    String id,
    String eventId,
    String eventName,
    String eventWorkTypeId,
    String eventWorkTypeName,
    String eventWorkTypeLevelCode,
    String eventWorkTypeLevelName,
    String stageId,
    String stageName,
    String title,
    JsonNode sourceScope,
    JsonNode questionTypeCounts,
    Integer questionCount,
    String status,
    String deliveryStatus,
    LocalDateTime createdAt) {
}
