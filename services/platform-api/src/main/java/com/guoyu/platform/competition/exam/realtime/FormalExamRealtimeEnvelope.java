package com.guoyu.platform.competition.exam.realtime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    @JsonSerialize(using = ToStringSerializer.class) Long seq,
    Instant occurredAt,
    Instant expiresAt,
    String target,
    Map<String, Object> payload) {

  public FormalExamRealtimeEnvelope withSeq(Long nextSeq) {
    return new FormalExamRealtimeEnvelope(
        eventId, commandId, type, aggregateId, examSessionId, candidateExamId,
        accessSessionId, generation, nextSeq, occurredAt, expiresAt, target, payload);
  }

  public FormalExamRealtimeEnvelope withPayload(Map<String, Object> nextPayload) {
    return new FormalExamRealtimeEnvelope(
        eventId, commandId, type, aggregateId, examSessionId, candidateExamId,
        accessSessionId, generation, seq, occurredAt, expiresAt, target, nextPayload);
  }
}
