package com.guoyu.platform.competition.messaging.service;

import com.guoyu.platform.competition.messaging.config.CompetitionMessagingProperties;
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
@ConditionalOnProperty(prefix = "platform.competition.messaging", name = "enabled", havingValue = "true")
public class CompetitionOutboxDispatcher {

  private final CompetitionOutboxDispatchService dispatchService;
  private final RabbitTemplate rabbitTemplate;
  private final CompetitionMessagingProperties properties;

  public CompetitionOutboxDispatcher(
      CompetitionOutboxDispatchService dispatchService,
      RabbitTemplate rabbitTemplate,
      CompetitionMessagingProperties properties) {
    this.dispatchService = dispatchService;
    this.rabbitTemplate = rabbitTemplate;
    this.properties = properties;
  }

  @Scheduled(fixedDelayString = "${platform.competition.messaging.dispatch-delay-millis:5000}")
  public void dispatch() {
    List<CompetitionOutboxEventEntity> events = dispatchService.claimDispatchableEvents();
    for (CompetitionOutboxEventEntity event : events) {
      try {
        CorrelationData correlationData = new CorrelationData(String.valueOf(event.getId()));
        rabbitTemplate.convertAndSend(
            properties.getExchange(), properties.getRoutingKey(), event.getPayloadJson(),
            message -> {
              message.getMessageProperties().setMessageId(String.valueOf(event.getId()));
              message.getMessageProperties().setType(event.getEventType());
              return message;
            }, correlationData);
        CorrelationData.Confirm confirm = correlationData.getFuture().get(10, TimeUnit.SECONDS);
        if (confirm == null || !confirm.isAck()) {
          throw new IllegalStateException(confirm == null ? "broker confirm timed out" : confirm.getReason());
        }
        dispatchService.markPublished(event.getId());
      } catch (Exception ex) {
        if (ex instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        log.warn("Competition outbox broker publish failed, outboxEventId={}, attempt={}",
            event.getId(), event.getAttemptCount(), ex);
        dispatchService.markFailed(event.getId(), event.getAttemptCount(), ex.getMessage());
      }
    }
  }
}
