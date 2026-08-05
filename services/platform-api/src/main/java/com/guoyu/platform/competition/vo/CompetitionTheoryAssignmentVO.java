package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionTheoryAssignmentVO(
    Long assignmentId,
    Long playerId,
    Long sessionId,
    String eventName,
    String workTypeName,
    String sessionName,
    LocalDateTime startAt,
    LocalDateTime endAt,
    String roomName,
    String address,
    String seatNo,
    String contestantNo,
    String contestantName,
    String orgName
) {
}
