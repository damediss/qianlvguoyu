package com.guoyu.platform.competition.messaging.service;

import com.guoyu.platform.competition.messaging.config.CompetitionScoringMessagingProperties;
import com.guoyu.platform.competition.messaging.entity.CompetitionOutboxEventEntity;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "platform.competition.formal-exam.scoring-messaging",
    name = "enabled", havingValue = "true", matchIfMissing = false)
public class CompetitionScoringOutboxDispatcher {
  private final CompetitionOutboxDispatchService dispatchService;
  private final RabbitTemplate rabbitTemplate;
  private final CompetitionScoringMessagingProperties properties;

  public CompetitionScoringOutboxDispatcher(CompetitionOutboxDispatchService dispatchService,
      RabbitTemplate rabbitTemplate, CompetitionScoringMessagingProperties properties) {
    this.dispatchService = dispatchService;
    this.rabbitTemplate = rabbitTemplate;
    this.properties = properties;
  }

  @Scheduled(fixedDelayString =
      "${platform.competition.formal-exam.scoring-messaging.dispatch-delay-millis:100}")
  public void dispatch() {
    dispatchChannel("scoring");
    dispatchChannel("score_pipeline");
  }

  private void dispatchChannel(String channel) {
    List<CompetitionOutboxEventEntity> events =
        dispatchService.claimDispatchableEvents(channel, properties.getBatchSize());
    for (CompetitionOutboxEventEntity event : events) {
      try {
        CorrelationData correlation = new CorrelationData(String.valueOf(event.getId()));
        rabbitTemplate.convertAndSend(properties.getExchange(), event.getRoutingKey(),
            event.getPayloadJson(), message -> {
              message.getMessageProperties().setMessageId(String.valueOf(event.getId()));
              message.getMessageProperties().setType(event.getEventType());
              return message;
            }, correlation);
        CorrelationData.Confirm confirm = correlation.getFuture().get(10, TimeUnit.SECONDS);
        if (confirm == null || !confirm.isAck()) {
          throw new IllegalStateException(confirm == null ? "broker confirm timed out" : confirm.getReason());
        }
        dispatchService.markPublished(event.getId());
      } catch (Exception ex) {
        if (ex instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        log.warn("Formal scoring outbox publish failed, eventId={}, attempt={}",
            event.getId(), event.getAttemptCount(), ex);
        // Broker outages must not strand scoring tasks: the durable outbox keeps retrying.
        dispatchService.markFailed(event.getId(), event.getAttemptCount(), ex.getMessage(),
            Integer.MAX_VALUE);
      }
    }
  }
}
