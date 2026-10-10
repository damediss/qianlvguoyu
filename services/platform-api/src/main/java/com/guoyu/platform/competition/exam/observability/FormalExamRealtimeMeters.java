package com.guoyu.platform.competition.exam.observability;

import com.guoyu.platform.competition.exam.realtime.FormalExamOutboxDispatcher;
import com.guoyu.platform.competition.exam.realtime.FormalExamRealtimeHub;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamOutboxMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Capacity gauges without PII. Values are sampled from in-process hub state and outbox counts.
 */
@Component
public class FormalExamRealtimeMeters {

  public FormalExamRealtimeMeters(
      MeterRegistry meterRegistry,
      FormalExamRealtimeHub hub,
      CompetitionFormalExamOutboxMapper outboxMapper,
      FormalExamOutboxDispatcher dispatcher) {
    Gauge.builder("formal_exam.ws.connections.active", hub, FormalExamRealtimeHub::openConnectionCount)
        .description("Open formal-exam WebSocket sessions on this node")
        .register(meterRegistry);
    Gauge.builder("formal_exam.ws.send.dropped", hub, value -> (double) value.droppedSendCount())
        .description("WS send-queue drops that closed a slow consumer")
        .register(meterRegistry);
    Gauge.builder("formal_exam.outbox.pending", outboxMapper, mapper -> count(mapper, "pending"))
        .register(meterRegistry);
    Gauge.builder("formal_exam.outbox.publishing", outboxMapper, mapper -> count(mapper, "publishing"))
        .register(meterRegistry);
    Gauge.builder("formal_exam.outbox.dropped", outboxMapper, mapper -> count(mapper, "dropped"))
        .register(meterRegistry);
    Gauge.builder("formal_exam.outbox.published", outboxMapper, mapper -> count(mapper, "published"))
        .register(meterRegistry);
    Gauge.builder(
            "formal_exam.outbox.oldest_pending_age_seconds",
            outboxMapper,
            mapper -> {
              Long age = mapper.oldestPendingAgeSeconds();
              return age == null ? 0d : age.doubleValue();
            })
        .register(meterRegistry);
    Gauge.builder(
            "formal_exam.outbox.publishing_lease_recoveries",
            dispatcher,
            value -> (double) value.publishingLeaseRecoveries())
        .description("Publishing rows reset to pending after lease expiry")
        .register(meterRegistry);
  }

  private static double count(CompetitionFormalExamOutboxMapper mapper, String status) {
    if (mapper == null || status == null) {
      return 0d;
    }
    return mapper.countByStatus(status);
  }
}
