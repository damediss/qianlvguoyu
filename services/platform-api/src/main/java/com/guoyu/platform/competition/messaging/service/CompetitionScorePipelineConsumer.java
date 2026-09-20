package com.guoyu.platform.competition.messaging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.entity.CompetitionScoreSourceEntity;
import com.guoyu.platform.competition.mapper.CompetitionScoreSourceMapper;
import com.guoyu.platform.competition.messaging.model.CompetitionScorePipelineRequestedEvent;
import com.guoyu.platform.competition.service.CompetitionAutomaticScorePipelineService;
import com.guoyu.platform.competition.messaging.config.CompetitionScoringMessagingProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "platform.competition.formal-exam.scoring-messaging",
    name = "enabled", havingValue = "true", matchIfMissing = false)
public class CompetitionScorePipelineConsumer {
  private final ObjectMapper objectMapper;
  private final CompetitionScoreSourceMapper sourceMapper;
  private final CompetitionAutomaticScorePipelineService pipelineService;
  private final CompetitionScorePipelineOutboxService outboxService;
  private final CompetitionScoringMessagingProperties properties;

  public CompetitionScorePipelineConsumer(ObjectMapper objectMapper,
      CompetitionScoreSourceMapper sourceMapper,
      CompetitionAutomaticScorePipelineService pipelineService,
      CompetitionScorePipelineOutboxService outboxService,
      CompetitionScoringMessagingProperties properties) {
    this.objectMapper = objectMapper;
    this.sourceMapper = sourceMapper;
    this.pipelineService = pipelineService;
    this.outboxService = outboxService;
    this.properties = properties;
  }

  @RabbitListener(queues = "#{@competitionScorePipelineQueue.name}",
      containerFactory = "competitionScoringListenerFactory")
  public void consume(String payload) {
    CompetitionScorePipelineRequestedEvent event;
    try {
      event = objectMapper.readValue(payload, CompetitionScorePipelineRequestedEvent.class);
    } catch (Exception ex) {
      throw new AmqpRejectAndDontRequeueException("invalid score pipeline payload", ex);
    }
    if (event.tenantId() == null || event.scoreSourceId() == null) {
      throw new AmqpRejectAndDontRequeueException("score pipeline event missing identifiers");
    }
    TenantContextHolder.setTenantId(event.tenantId());
    CompetitionScoreSourceEntity source = null;
    try {
      source = sourceMapper.selectById(event.scoreSourceId());
      if (source != null && "confirmed".equals(source.getConfirmationStatus())) {
        pipelineService.onConfirmedSource(source,
            source.getConfirmedBy() == null ? 0L : source.getConfirmedBy());
      }
    } catch (RuntimeException ex) {
      log.error("Score pipeline consumer failed, tenantId={}, sourceId={}",
          event.tenantId(), event.scoreSourceId(), ex);
      int nextAttempt = event.attemptNo() + 1;
      if (source != null && nextAttempt < properties.getMaxAttempts()) {
        outboxService.enqueueRetry(source,
            source.getConfirmedBy() == null ? 0L : source.getConfirmedBy(), nextAttempt);
      }
      throw new AmqpRejectAndDontRequeueException("score pipeline processing failed", ex);
    } finally {
      TenantContextHolder.clear();
    }
  }
}
