package com.guoyu.platform.competition.exam.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionFormalExamOutboxEntity;
import com.guoyu.platform.competition.mapper.CompetitionFormalExamOutboxMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class FormalExamOutboxDispatcher {

  private final CompetitionFormalExamOutboxMapper outboxMapper;
  private final StringRedisTemplate redis;
  private final FormalExamRealtimeHub hub;
  private final ObjectMapper objectMapper;
  private final FormalExamRealtimeProperties properties;
  private final Clock clock;

  public FormalExamOutboxDispatcher(
      CompetitionFormalExamOutboxMapper outboxMapper,
      @org.springframework.beans.factory.annotation.Autowired(required = false) StringRedisTemplate redis,
      FormalExamRealtimeHub hub,
      ObjectMapper objectMapper,
      FormalExamRealtimeProperties properties,
      Clock clock) {
    this.outboxMapper = outboxMapper;
    this.redis = redis;
    this.hub = hub;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${platform.competition.formal-exam.realtime.dispatch-delay-millis:1000}")
  public void dispatchPending() {
    if (!properties.isEnabled()) {
      return;
    }
    List<CompetitionFormalExamOutboxEntity> pending =
        outboxMapper.selectPending(Math.max(1, properties.getDispatchBatchSize()));
    if (pending == null || pending.isEmpty()) {
      return;
    }
    for (CompetitionFormalExamOutboxEntity row : pending) {
      if (row == null || row.getId() == null) {
        continue;
      }
      publishOne(row.getId());
    }
  }

  public void publishOne(Long outboxId) {
    if (outboxId == null) {
      return;
    }
    CompetitionFormalExamOutboxEntity row = outboxMapper.selectByIdIgnoreTenant(outboxId);
    if (row == null || !"pending".equals(row.getStatus())) {
      if (row != null && "publishing".equals(row.getStatus())) {
        deliver(row);
      }
      return;
    }
    Long previousTenant = TenantContextHolder.getTenantId();
    try {
      TenantContextHolder.setTenantId(row.getTenantId());
      if (outboxMapper.markPublishing(row.getId(), LocalDateTime.now(clock)) != 1) {
        return;
      }
      row.setAttempts((row.getAttempts() == null ? 0 : row.getAttempts()) + 1);
      deliver(row);
    } finally {
      if (previousTenant == null) {
        TenantContextHolder.clear();
      } else {
        TenantContextHolder.setTenantId(previousTenant);
      }
    }
  }

  private void deliver(CompetitionFormalExamOutboxEntity row) {
    LocalDateTime now = LocalDateTime.now(clock);
    try {
      FormalExamRealtimeEnvelope envelope =
          objectMapper.readValue(row.getPayloadJson(), FormalExamRealtimeEnvelope.class);
      // Always push to this JVM's WebSocket hub. Redis pub/sub is fan-out for other
      // nodes only; relying on it alone drops console/player events when the local
      // listener is not receiving (common with pooled Lettuce).
      hub.deliver(envelope, row.getPayloadJson());
      if (redis != null) {
        redis.convertAndSend(FormalExamRealtimeTypes.CHANNEL, row.getPayloadJson());
      }
      outboxMapper.markFinished(row.getId(), "published", now, null);
    } catch (Exception ex) {
      int attempts = row.getAttempts() == null ? 1 : row.getAttempts();
      if (attempts >= Math.max(1, properties.getDispatchMaxAttempts())) {
        log.warn("Formal exam realtime outbox dropped, outboxId={}, type={}",
            row.getId(), row.getEventType());
        outboxMapper.markFinished(row.getId(), "dropped", now, safeError(ex));
        return;
      }
      outboxMapper.markPendingRetry(row.getId(), safeError(ex), now);
    }
  }

  private static String safeError(Exception ex) {
    String message = ex.getMessage();
    if (!StringUtils.hasText(message)) {
      return ex.getClass().getSimpleName();
    }
    return message.length() <= 500 ? message : message.substring(0, 500);
  }
}
