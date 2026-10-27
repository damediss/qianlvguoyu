package com.guoyu.platform.competition.messaging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.competition.mapper.CompetitionNoticeMapper;
import com.guoyu.platform.competition.mapper.CompetitionRegistrationMapper;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import com.guoyu.platform.competition.messaging.mapper.CompetitionPlayerMessageMapper;
import com.guoyu.platform.competition.messaging.model.CompetitionPlayerMessageAvailableBatch;
import com.guoyu.platform.competition.messaging.model.CompetitionPlayerMessageAvailableBatch.Recipient;
import com.guoyu.platform.competition.messaging.model.CompetitionNoticePublishedEvent;
import java.util.Locale;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates player inbox records. Duplicate deliveries are harmless because the database unique key wins. */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "platform.competition.messaging", name = "enabled", havingValue = "true")
public class CompetitionNoticeDeliveryConsumer {

  private static final int REALTIME_RECIPIENTS_PER_BATCH = 500;

  private final ObjectMapper objectMapper;
  private final CompetitionRegistrationMapper registrationMapper;
  private final CompetitionNoticeInboxCompensationService inboxService;
  private final CompetitionNoticeMapper noticeMapper;
  private final CompetitionOutboxApplicationService outboxService;

  @Autowired
  public CompetitionNoticeDeliveryConsumer(
      ObjectMapper objectMapper,
      CompetitionRegistrationMapper registrationMapper,
      CompetitionNoticeInboxCompensationService inboxService,
      CompetitionNoticeMapper noticeMapper,
      CompetitionOutboxApplicationService outboxService) {
    this.objectMapper = objectMapper;
    this.registrationMapper = registrationMapper;
    this.inboxService = inboxService;
    this.noticeMapper = noticeMapper;
    this.outboxService = outboxService;
  }

  CompetitionNoticeDeliveryConsumer(
      ObjectMapper objectMapper,
      CompetitionRegistrationMapper registrationMapper,
      CompetitionPlayerMessageMapper playerMessageMapper) {
    this(objectMapper, registrationMapper,
        new CompetitionNoticeInboxCompensationService(null, playerMessageMapper), null, null);
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
      List<Recipient> recipients = new ArrayList<>();
      for (Long playerId : playerIds) {
        if (playerId == null) {
          log.warn("Competition notice delivery query returned null player, tenantId={}, eventId={}",
              event.tenantId(), event.eventId());
          continue;
        }
        CompetitionNoticeInboxCompensationService.DeliveryResult result =
            inboxService.deliverPublishedNoticeWithResult(
                event.tenantId(), event.eventId(), event.noticeId(), playerId, now);
        if (result.inserted()) {
          recipients.add(new Recipient(playerId, result.messageId()));
        }
      }
      enqueueRealtimeEvents(event, recipients);
    } finally {
      TenantContextHolder.clear();
    }
  }

  private void enqueueRealtimeEvents(
      CompetitionNoticePublishedEvent event, List<Recipient> recipients) {
    if (outboxService == null || noticeMapper == null || recipients.isEmpty()) {
      return;
    }
    CompetitionNoticeEntity notice = noticeMapper.selectById(event.noticeId());
    if (notice == null || !event.tenantId().equals(notice.getTenantId())
        || !event.eventId().equals(notice.getEventId()) || !"published".equals(notice.getStatus())) {
      log.warn("Published notice metadata unavailable for realtime event, tenantId={}, eventId={}, noticeId={}",
          event.tenantId(), event.eventId(), event.noticeId());
      return;
    }

    String title = notice.getTitle() == null ? "赛事公告" : notice.getTitle();
    String summary = summarize(notice.getContent());
    String contentType = notice.getContentType() == null
        ? "markdown" : notice.getContentType().toLowerCase(Locale.ROOT);
    int batchNumber = 0;
    for (int start = 0; start < recipients.size(); start += REALTIME_RECIPIENTS_PER_BATCH) {
      int end = Math.min(start + REALTIME_RECIPIENTS_PER_BATCH, recipients.size());
      CompetitionPlayerMessageAvailableBatch batch = new CompetitionPlayerMessageAvailableBatch(
          event.tenantId(), event.eventId(), event.noticeId(), title, summary, contentType,
          event.publishedAt(), recipients.subList(start, end));
      outboxService.enqueuePlayerMessageAvailable(batch, batchNumber++);
    }
  }

  private static String summarize(String content) {
    if (content == null || content.isBlank()) {
      return "平台发布了一条赛事公告";
    }
    String summary = content
        .replaceAll("!\\[[^\\]]*\\]\\(notice-asset://\\d+\\)", "【图片】")
        .replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1")
        .replaceAll("<[^>]*>", " ")
        .replaceAll("[#*_`~>]", " ")
        .replaceAll("\\s+", " ")
        .trim();
    if (summary.length() > 180) {
      return summary.substring(0, 180) + "…";
    }
    return summary.isEmpty() ? "平台发布了一条赛事公告" : summary;
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
