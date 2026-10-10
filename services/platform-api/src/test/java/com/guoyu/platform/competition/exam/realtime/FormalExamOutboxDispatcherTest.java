package com.guoyu.platform.competition.exam.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;

class FormalExamOutboxDispatcherTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-10T01:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private final CompetitionFormalExamOutboxMapper outboxMapper =
      Mockito.mock(CompetitionFormalExamOutboxMapper.class);
  private final FormalExamRealtimeHub hub = Mockito.mock(FormalExamRealtimeHub.class);
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final FormalExamRealtimeProperties properties = new FormalExamRealtimeProperties();

  @Test
  void redisPublishAlsoDeliversLocallyAndMarksPublishedAfterRedis() throws Exception {
    properties.setNodeId("api-1");
    StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
    FormalExamOutboxDispatcher dispatcher = new FormalExamOutboxDispatcher(
        outboxMapper, redis, hub, objectMapper, properties, CLOCK);
    CompetitionFormalExamOutboxEntity row = pendingRow();
    FormalExamRealtimeEnvelope envelope = new FormalExamRealtimeEnvelope(
        "e1", null, FormalExamRealtimeTypes.SESSION_STATUS_CHANGED,
        "9001", "9001", null, null, null, 1L, null, null,
        FormalExamRealtimeTypes.TARGET_SESSION, java.util.Map.of("status", "running"));
    when(outboxMapper.selectPendingHeadByIdForUpdate(9L)).thenReturn(row);
    when(outboxMapper.markPublishing(eq(9L), any(LocalDateTime.class))).thenReturn(1);

    dispatcher.publishOne(9L);

    verify(hub).deliver(any(FormalExamRealtimeEnvelope.class), eq(row.getPayloadJson()));
    ArgumentCaptor<String> bus = ArgumentCaptor.forClass(String.class);
    verify(redis).convertAndSend(eq(FormalExamRealtimeTypes.CHANNEL), bus.capture());
    FormalExamRealtimeBusMessages.Parsed parsed =
        FormalExamRealtimeBusMessages.parse(objectMapper, bus.getValue());
    org.assertj.core.api.Assertions.assertThat(parsed.publisherNodeId()).isEqualTo("api-1");
    verify(outboxMapper).markFinished(eq(9L), eq("published"), any(LocalDateTime.class), eq(null));
    org.assertj.core.api.Assertions.assertThat(envelope.eventId()).isEqualTo("e1");
  }

  @Test
  void missingRedisFallsBackToLocalHub() {
    FormalExamOutboxDispatcher dispatcher = new FormalExamOutboxDispatcher(
        outboxMapper, null, hub, objectMapper, properties, CLOCK);
    CompetitionFormalExamOutboxEntity row = pendingRow();
    when(outboxMapper.selectPendingHeadByIdForUpdate(9L)).thenReturn(row);
    when(outboxMapper.markPublishing(eq(9L), any(LocalDateTime.class))).thenReturn(1);

    dispatcher.publishOne(9L);

    verify(hub).deliver(any(FormalExamRealtimeEnvelope.class), eq(row.getPayloadJson()));
    verify(outboxMapper).markFinished(eq(9L), eq("published"), any(LocalDateTime.class), eq(null));
  }

  @Test
  void publishingRowIsNotDeliveredAgain() {
    FormalExamOutboxDispatcher dispatcher = new FormalExamOutboxDispatcher(
        outboxMapper, null, hub, objectMapper, properties, CLOCK);
    CompetitionFormalExamOutboxEntity row = pendingRow();
    row.setStatus("publishing");
    when(outboxMapper.selectPendingHeadByIdForUpdate(9L)).thenReturn(row);

    dispatcher.publishOne(9L);

    verify(outboxMapper, never()).markPublishing(any(), any());
    verify(hub, never()).deliver(any(), anyString());
  }

  @Test
  void recoverExpiredPublishingBeforeSelectingPending() {
    FormalExamOutboxDispatcher dispatcher = new FormalExamOutboxDispatcher(
        outboxMapper, null, hub, objectMapper, properties, CLOCK);
    when(outboxMapper.selectExpiredPublishingForUpdate(any(), eq(50)))
        .thenReturn(java.util.List.of(1L, 2L));
    when(outboxMapper.resetExpiredPublishingBatch(any(), any())).thenReturn(2);
    when(outboxMapper.selectPendingHeadsForUpdate(1)).thenReturn(java.util.List.of());

    dispatcher.dispatchPending();

    verify(outboxMapper).selectExpiredPublishingForUpdate(any(LocalDateTime.class), eq(50));
    verify(outboxMapper).resetExpiredPublishingBatch(any(), any(LocalDateTime.class));
    org.assertj.core.api.Assertions.assertThat(dispatcher.publishingLeaseRecoveries()).isEqualTo(2);
  }

  private static CompetitionFormalExamOutboxEntity pendingRow() {
    CompetitionFormalExamOutboxEntity row = new CompetitionFormalExamOutboxEntity();
    row.setId(9L);
    row.setTenantId(1L);
    row.setStatus("pending");
    row.setAttempts(0);
    row.setEventType(FormalExamRealtimeTypes.SESSION_STATUS_CHANGED);
    row.setPayloadJson("""
        {"eventId":"e1","type":"session.status.changed","examSessionId":"9001","seq":"1","target":"examSession","payload":{"status":"running"}}
        """);
    return row;
  }
}
