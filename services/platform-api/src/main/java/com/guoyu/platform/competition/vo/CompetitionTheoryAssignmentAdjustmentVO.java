package com.guoyu.platform.competition.vo;

import java.time.LocalDateTime;

public record CompetitionTheoryAssignmentAdjustmentVO(
    Long id,
    Long assignmentId,
    Long contestantProfileId,
    Long fromSessionId,
    Long fromSessionRoomId,
    String fromSeatNo,
    Long toSessionId,
    Long toSessionRoomId,
    String toSeatNo,
    String reason,
    Long adjustedBy,
    LocalDateTime adjustedAt
) {
}
