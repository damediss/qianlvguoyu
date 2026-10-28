package com.guoyu.platform.competition.prepractice.vo;

import java.time.LocalDateTime;

public record CompetitionPrePracticeDispatchBatchVO(
    String id,
    String taskId,
    String taskTitle,
    String eventId,
    String eventName,
    String eventWorkTypeId,
    String eventWorkTypeName,
    String eventWorkTypeLevelCode,
    String eventWorkTypeLevelName,
    String stageId,
    String stageName,
    Integer recipientCount,
    LocalDateTime pushedAt) {
}
