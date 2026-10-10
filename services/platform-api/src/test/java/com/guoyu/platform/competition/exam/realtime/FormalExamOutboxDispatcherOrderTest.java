package com.guoyu.platform.competition.exam.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.data.redis.core.StringRedisTemplate;

class FormalExamOutboxDispatcherOrderTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-10T01:00:00Z"), ZoneId.of("Asia/Shanghai"));

  @Test
  void twoDispatchersPublishStrictlyIncreasingSeqAndClientsDedupeEventId() throws Exception {
    CompetitionFormalExamOutboxMapper mapper = Mockito.mock(CompetitionFormalExamOutboxMapper.class);
    ConcurrentHashMap<Long, CompetitionFormalExamOutboxEntity> rows = new ConcurrentHashMap<>();
    for (long seq = 1; seq <= 6; seq++) {
      rows.put(seq, pending(seq));
    }
    when(mapper.selectPendingHeadByIdForUpdate(any()))
        .thenAnswer(inv -> copy(rows.get(inv.getArgument(0))));
    when(mapper.markPublishing(any(), any(LocalDateTime.class))).thenAnswer(inv -> claim(rows, inv));
    when(mapper.markFinished(any(), eq("published"), any(), any())).thenAnswer(inv -> {
      CompetitionFormalExamOutboxEntity row = rows.get(inv.getArgument(0));
      if (row == null || !"publishing".equals(row.getStatus())) {
        return 0;
      }
      row.setStatus("published");
      return 1;
    });

    CopyOnWriteArrayList<Long> redisSeq = new CopyOnWriteArrayList<>();
    StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
    Mockito.doAnswer(inv -> {
      String json = inv.getArgument(1);
      FormalExamRealtimeBusMessages.Parsed parsed =
          FormalExamRealtimeBusMessages.parse(new ObjectMapper().findAndRegisterModules(), json);
      redisSeq.add(parsed.envelope().seq());
      return 1L;
    }).when(redis).convertAndSend(eq(FormalExamRealtimeTypes.CHANNEL), any());

    FormalExamRealtimeHub hub = Mockito.mock(FormalExamRealtimeHub.class);
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    FormalExamRealtimeProperties properties = new FormalExamRealtimeProperties();
    properties.setNodeId("api-1");
    FormalExamOutboxDispatcher first = new FormalExamOutboxDispatcher(
        mapper, redis, hub, objectMapper, properties, CLOCK);
    FormalExamOutboxDispatcher second = new FormalExamOutboxDispatcher(
        mapper, redis, hub, objectMapper, properties, CLOCK);

    for (int round = 0; round < 6; round++) {
      first.publishOne(1L + round);
      second.publishOne(1L + round);
    }

    assertThat(redisSeq).containsExactly(1L, 2L, 3L, 4L, 5L, 6L);
    Set<String> seen = ConcurrentHashMap.newKeySet();
    List<String> applied = new ArrayList<>();
    for (Long seq : redisSeq) {
      String eventId = "e" + seq;
      if (seen.add(eventId)) {
        applied.add(eventId);
      }
    }
    assertThat(applied).containsExactly("e1", "e2", "e3", "e4", "e5", "e6");
    verify(hub, times(6)).deliver(any(), any());
    verify(mapper, never()).markFinished(any(), eq("dropped"), any(), any());
  }

  private static int claim(
      ConcurrentHashMap<Long, CompetitionFormalExamOutboxEntity> rows, InvocationOnMock inv) {
    long id = inv.getArgument(0);
    synchronized (rows) {
      CompetitionFormalExamOutboxEntity target = rows.get(id);
      if (target == null || !"pending".equals(target.getStatus())) {
        return 0;
      }
      boolean blocked = rows.values().stream().anyMatch(prior ->
          prior.getId() != id
              && prior.getSeq() < target.getSeq()
              && ("pending".equals(prior.getStatus()) || "publishing".equals(prior.getStatus())));
      if (blocked) {
        return 0;
      }
      target.setStatus("publishing");
      return 1;
    }
  }

  private static CompetitionFormalExamOutboxEntity pending(long seq) {
    CompetitionFormalExamOutboxEntity row = new CompetitionFormalExamOutboxEntity();
    row.setId(seq);
    row.setTenantId(1L);
    row.setExamSessionId(9001L);
    row.setSeq(seq);
    row.setStatus("pending");
    row.setAttempts(0);
    row.setEventId("e" + seq);
    row.setEventType(FormalExamRealtimeTypes.SESSION_STATUS_CHANGED);
    row.setPayloadJson("""
        {"eventId":"e%s","type":"session.status.changed","examSessionId":"9001","seq":"%s","target":"examSession","payload":{}}
        """.formatted(seq, seq));
    return row;
  }

  private static CompetitionFormalExamOutboxEntity copy(CompetitionFormalExamOutboxEntity row) {
    if (row == null) {
      return null;
    }
    CompetitionFormalExamOutboxEntity copy = pending(row.getSeq());
    copy.setStatus(row.getStatus());
    copy.setAttempts(row.getAttempts());
    return copy;
  }
}
