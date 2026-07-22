package com.guoyu.platform.competition.messaging.service;

import com.guoyu.platform.competition.messaging.config.CompetitionMessagingProperties;
import com.guoyu.platform.competition.messaging.entity.CompetitionOutboxEventEntity;
import com.guoyu.platform.competition.messaging.mapper.CompetitionOutboxEventMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompetitionOutboxDispatchService {

  private final CompetitionOutboxEventMapper outboxEventMapper;
  private final CompetitionMessagingProperties properties;

  public CompetitionOutboxDispatchService(
      CompetitionOutboxEventMapper outboxEventMapper,
      CompetitionMessagingProperties properties) {
    this.outboxEventMapper = outboxEventMapper;
    this.properties = properties;
  }

  @Transactional
  public List<CompetitionOutboxEventEntity> claimDispatchableEvents() {
    LocalDateTime now = LocalDateTime.now();
    List<CompetitionOutboxEventEntity> events = outboxEventMapper.selectDispatchableForUpdate(
        now, now.minusMinutes(10), properties.getBatchSize());
    if (events == null || events.isEmpty()) {
      return List.of();
    }
    events.removeIf(event -> outboxEventMapper.markPublishing(event.getId(), now) != 1);
    events.forEach(event -> {
      event.setStatus("publishing");
      event.setAttemptCount(event.getAttemptCount() + 1);
    });
    return events;
  }

  @Transactional
  public void markPublished(Long outboxEventId) {
    outboxEventMapper.markPublished(outboxEventId, LocalDateTime.now());
  }

  @Transactional
  public void markFailed(Long outboxEventId, int attemptCount, String error) {
    LocalDateTime now = LocalDateTime.now();
    boolean terminal = attemptCount >= properties.getMaxAttempts();
    outboxEventMapper.markRetryOrFailed(
        outboxEventId,
        terminal ? "failed" : "retry",
        terminal ? now : now.plusSeconds(backoffSeconds(attemptCount)),
        abbreviate(error),
        now);
  }

  private long backoffSeconds(int attemptCount) {
    return Math.min(300L, 1L << Math.min(Math.max(attemptCount, 0), 8));
  }

  private String abbreviate(String error) {
    if (error == null) {
      return "broker publish failed";
    }
    return error.length() <= 500 ? error : error.substring(0, 500);
  }
}
