package com.guoyu.platform.competition.messaging.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.messaging.entity.CompetitionPlayerMessageEntity;
import com.guoyu.platform.competition.messaging.mapper.CompetitionPlayerMessageMapper;
import com.guoyu.platform.competition.messaging.model.CompetitionNoticePublishedEvent;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates player inbox records. Duplicate deliveries are harmless because the database unique key wins. */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "platform.competition.messaging", name = "enabled", havingValue = "true")
public class CompetitionNoticeDeliveryConsumer {

  private final ObjectMapper objectMapper;
  private final CompetitionRegistrationMapper registrationMapper;
  private final CompetitionPlayerMessageMapper playerMessageMapper;

  public CompetitionNoticeDeliveryConsumer(
      ObjectMapper objectMapper,
      CompetitionRegistrationMapper registrationMapper,
      CompetitionPlayerMessageMapper playerMessageMapper) {
    this.objectMapper = objectMapper;
    this.registrationMapper = registrationMapper;
    this.playerMessageMapper = playerMessageMapper;
  }

  @RabbitListener(queues = "#{@competitionNoticeDeliveryQueue.name}")
  @Transactional
  public void consume(String rawPayload) {
    CompetitionNoticePublishedEvent event = parse(rawPayload);
    TenantContextHolder.setTenantId(event.tenantId());
    try {
      List<Long> playerIds = registrationMapper.selectActivePlayerIdsByEvent(
          event.tenantId(), event.eventId());
      if (playerIds == null || playerIds.isEmpty()) {
        return;
      }
      LocalDateTime now = LocalDateTime.now();
      for (Long playerId : playerIds) {
        if (playerId == null) {
          log.warn("Competition notice delivery query returned null player, tenantId={}, eventId={}",
              event.tenantId(), event.eventId());
          continue;
        }
        CompetitionPlayerMessageEntity message = new CompetitionPlayerMessageEntity();
        message.setId(IdWorker.getId());
        message.setTenantId(event.tenantId());
        message.setPlayerId(playerId);
        message.setEventId(event.eventId());
        message.setNoticeId(event.noticeId());
        message.setType("notice_published");
        message.setReceivedAt(now);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        message.setDeleted(0);
        playerMessageMapper.insertIgnoreDuplicate(message);
      }
    } finally {
      TenantContextHolder.clear();
    }
  }

  private CompetitionNoticePublishedEvent parse(String rawPayload) {
    try {
      CompetitionNoticePublishedEvent event = objectMapper.readValue(
          rawPayload, CompetitionNoticePublishedEvent.class);
      if (event.tenantId() == null || event.eventId() == null || event.noticeId() == null) {
        throw new IllegalArgumentException("missing required event identifiers");
      }
      return event;
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      log.error("Competition notice delivery payload is invalid; send to dead letter queue", ex);
      throw new AmqpRejectAndDontRequeueException("invalid competition notice event", ex);
    }
  }
}
