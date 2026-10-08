package com.guoyu.platform.competition.draw.vo;

import java.time.LocalDateTime;
import java.util.List;

public record CompetitionPlayerDrawTaskVO(
    String id,
    String eventId,
    String stageId,
    String stageName,
    Integer playerCount,
    Integer drawnCount,
    String status,
    LocalDateTime createdAt,
    List<CompetitionPlayerDrawSlot> slots) {
}
