package com.guoyu.platform.competition.exam.vo;

import java.time.LocalDateTime;

public record CompetitionTheoryMonitorVO(
    Long attemptId,
    Long assignmentId,
    Long playerId,
    String contestantNo,
    String contestantName,
    String orgName,
    String sessionName,
    String roomName,
    String seatNo,
    String status,
    LocalDateTime startedAt,
    LocalDateTime lastSavedAt,
    LocalDateTime submittedAt,
    boolean examSessionOnline) {
}
