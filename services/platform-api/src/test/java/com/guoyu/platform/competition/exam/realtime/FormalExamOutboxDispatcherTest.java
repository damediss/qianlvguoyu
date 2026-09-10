package com.guoyu.platform.competition.exam.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.entity.CompetitionFormalExamOutboxEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamOutboxMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;

class FormalExamOutboxDispatcherTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-10T01:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private final CompetitionFormalExamOutboxMapper outboxMapper =
      Mockito.mock(CompetitionFormalExamOutboxMapper.class);
  private final FormalExamRealtimeHub hub = Mockito.mock(FormalExamRealtimeHub.class);
  private final ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
  private final FormalExamRealtimeProperties properties = new FormalExamRealtimeProperties();

  @Test
  void redisPublishAlsoDeliversLocally() throws Exception {
    StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
    FormalExamOutboxDispatcher dispatcher = new FormalExamOutboxDispatcher(
        outboxMapper, redis, hub, objectMapper, properties, CLOCK);
    CompetitionFormalExamOutboxEntity row = pendingRow();
    FormalExamRealtimeEnvelope envelope = new FormalExamRealtimeEnvelope(
        "e1", null, FormalExamRealtimeTypes.SESSION_STATUS_CHANGED,
        "9001", "9001", null, null, null, 1, null, null,
        FormalExamRealtimeTypes.TARGET_SESSION, java.util.Map.of("status", "running"));
    when(outboxMapper.selectByIdIgnoreTenant(9L)).thenReturn(row);
    when(outboxMapper.markPublishing(eq(9L), any(LocalDateTime.class))).thenReturn(1);
    when(objectMapper.readValue(eq(row.getPayloadJson()), eq(FormalExamRealtimeEnvelope.class)))
        .thenReturn(envelope);

    dispatcher.publishOne(9L);

    verify(hub).deliver(envelope, row.getPayloadJson());
    verify(redis).convertAndSend(FormalExamRealtimeTypes.CHANNEL, row.getPayloadJson());
    verify(outboxMapper).markFinished(eq(9L), eq("published"), any(LocalDateTime.class), eq(null));
  }

  @Test
  void missingRedisFallsBackToLocalHub() throws Exception {
    FormalExamOutboxDispatcher dispatcher = new FormalExamOutboxDispatcher(
        outboxMapper, null, hub, objectMapper, properties, CLOCK);
    CompetitionFormalExamOutboxEntity row = pendingRow();
    FormalExamRealtimeEnvelope envelope = new FormalExamRealtimeEnvelope(
        "e1", null, FormalExamRealtimeTypes.SESSION_STATUS_CHANGED,
        "9001", "9001", null, null, null, 1, null, null,
        FormalExamRealtimeTypes.TARGET_SESSION, java.util.Map.of("status", "running"));
    when(outboxMapper.selectByIdIgnoreTenant(9L)).thenReturn(row);
    when(outboxMapper.markPublishing(eq(9L), any(LocalDateTime.class))).thenReturn(1);
    when(objectMapper.readValue(eq(row.getPayloadJson()), eq(FormalExamRealtimeEnvelope.class)))
        .thenReturn(envelope);

    dispatcher.publishOne(9L);

    verify(hub).deliver(any(FormalExamRealtimeEnvelope.class), eq(row.getPayloadJson()));
    verify(outboxMapper).markFinished(eq(9L), eq("published"), any(LocalDateTime.class), eq(null));
  }

  private static CompetitionFormalExamOutboxEntity pendingRow() {
    CompetitionFormalExamOutboxEntity row = new CompetitionFormalExamOutboxEntity();
    row.setId(9L);
    row.setTenantId(1L);
    row.setStatus("pending");
    row.setAttempts(0);
    row.setEventType(FormalExamRealtimeTypes.SESSION_STATUS_CHANGED);
    row.setPayloadJson("""
        {"eventId":"e1","type":"session.status.changed","examSessionId":"9001","target":"examSession","payload":{"status":"running"}}
        """);
    return row;
  }
}
