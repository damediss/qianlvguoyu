package com.guoyu.platform.competition.messaging.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.competition.messaging.model.CompetitionPlayerMessageAvailableBatch;
import com.guoyu.platform.competition.messaging.model.CompetitionPlayerMessageAvailableBatch.Recipient;
import com.guoyu.platform.competition.messaging.model.CompetitionPlayerMessageAvailableEvent;
import com.guoyu.platform.competition.player.realtime.CompetitionPlayerRealtimeHub;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Delivers a node-broadcast event only to matching local player sessions. */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "platform.competition.messaging", name = "enabled", havingValue = "true")
public class CompetitionPlayerMessageRealtimeConsumer {

  private static final DateTimeFormatter VERSION_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

  private final ObjectMapper objectMapper;
  private final CompetitionPlayerRealtimeHub hub;

  public CompetitionPlayerMessageRealtimeConsumer(
      ObjectMapper objectMapper, CompetitionPlayerRealtimeHub hub) {
    this.objectMapper = objectMapper;
    this.hub = hub;
  }

  @RabbitListener(queues = "#{@competitionPlayerRealtimeQueue.name}")
  public void consume(String rawPayload) {
    try {
      CompetitionPlayerMessageAvailableBatch batch = objectMapper.readValue(
          rawPayload, CompetitionPlayerMessageAvailableBatch.class);
      if (batch == null || batch.tenantId() == null || batch.recipients() == null) {
        log.warn("Competition player realtime payload is missing required fields");
        return;
      }
      for (Recipient recipient : batch.recipients()) {
        if (recipient == null || recipient.playerId() == null || recipient.messageId() == null) {
          continue;
        }
        LocalDateTime publishedAt = batch.publishedAt() == null ? LocalDateTime.now() : batch.publishedAt();
        String messageId = recipient.messageId().toString();
        CompetitionPlayerMessageAvailableEvent event = new CompetitionPlayerMessageAvailableEvent(
            CompetitionPlayerMessageAvailableEvent.TYPE,
            messageId,
            batch.eventId() == null ? null : batch.eventId().toString(),
            batch.noticeId() == null ? null : batch.noticeId().toString(),
            batch.title(),
            batch.summary(),
            batch.contentType(),
            publishedAt,
            1,
            VERSION_TIME_FORMAT.format(publishedAt) + "-" + messageId);
        hub.sendToPlayer(batch.tenantId(), recipient.playerId(), objectMapper.writeValueAsString(event));
      }
    } catch (Exception ex) {
      log.warn("Competition player realtime payload ignored: {}", ex.toString());
    }
  }
}
