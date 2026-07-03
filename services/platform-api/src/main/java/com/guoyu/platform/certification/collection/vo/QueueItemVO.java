package com.guoyu.platform.certification.collection.vo;

import java.time.LocalDateTime;

public record QueueItemVO(
    Long queueId,
    Long taskId,
    Long candidateId,
    Integer queueNo,
    String name,
    String idCardLast4,
    String unitName,
    String currentStatus,
    String currentStatusLabel,
    String queueStatus,
    String queueStatusLabel,
    String matchMethod,
    String matchMethodLabel,
    LocalDateTime joinedAt,
    LocalDateTime startedAt
) {
}
