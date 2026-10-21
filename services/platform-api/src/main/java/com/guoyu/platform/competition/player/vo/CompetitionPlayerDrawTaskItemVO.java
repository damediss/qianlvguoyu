package com.guoyu.platform.competition.player.vo;

import java.time.LocalDateTime;

public record CompetitionPlayerDrawTaskItemVO(
    String taskId,
    String eventId,
    String eventName,
    String stageId,
    String stageName,
    String workTypeName,
    String levelName,
    String status,
    Integer appearanceOrder,
    LocalDateTime drawnAt,
    LocalDateTime createdAt) {
}
