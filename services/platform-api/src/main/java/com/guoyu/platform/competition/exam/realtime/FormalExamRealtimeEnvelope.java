package com.guoyu.platform.competition.exam.realtime;

import java.time.Instant;
import java.util.Map;

public record FormalExamRealtimeEnvelope(
    String eventId,
    String commandId,
    String type,
    String aggregateId,
    String examSessionId,
    String candidateExamId,
    String accessSessionId,
    Integer generation,
    Integer seq,
    Instant occurredAt,
    Instant expiresAt,
    String target,
    Map<String, Object> payload) {
}
