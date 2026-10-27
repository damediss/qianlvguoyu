package com.guoyu.platform.competition.messaging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import com.guoyu.platform.competition.messaging.config.CompetitionMessagingProperties;
import com.guoyu.platform.competition.messaging.entity.CompetitionOutboxEventEntity;
import com.guoyu.platform.competition.messaging.mapper.CompetitionOutboxEventMapper;
import com.guoyu.platform.competition.messaging.model.CompetitionNoticePublishedEvent;
import com.guoyu.platform.competition.messaging.model.CompetitionPlayerMessageAvailableBatch;
import com.guoyu.platform.competition.messaging.model.CompetitionPlayerMessageAvailableEvent;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionOutboxApplicationService {

  public static final String NOTICE_PUBLISHED_EVENT_TYPE = "competition.notice.published.v1";
  public static final String PLAYER_MESSAGE_AVAILABLE_EVENT_TYPE =
      CompetitionPlayerMessageAvailableEvent.TYPE;
  private static final String NOTICE_AGGREGATE_TYPE = "competition_notice";

  private final CompetitionOutboxEventMapper outboxEventMapper;
  private final ObjectMapper objectMapper;
  private final CompetitionMessagingProperties properties;
  private final CompetitionOutboxWakeup outboxWakeup;

  @Autowired
  public CompetitionOutboxApplicationService(
      CompetitionOutboxEventMapper outboxEventMapper,
      ObjectMapper objectMapper,
      CompetitionMessagingProperties properties,
      ObjectProvider<CompetitionOutboxWakeup> outboxWakeupProvider) {
    this.outboxEventMapper = outboxEventMapper;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.outboxWakeup = outboxWakeupProvider.getIfAvailable();
  }

  public CompetitionOutboxApplicationService(
      CompetitionOutboxEventMapper outboxEventMapper,
      ObjectMapper objectMapper,
      CompetitionMessagingProperties properties) {
    this(outboxEventMapper, objectMapper, properties, null);
  }

  public CompetitionOutboxApplicationService(
      CompetitionOutboxEventMapper outboxEventMapper, ObjectMapper objectMapper) {
    this(outboxEventMapper, objectMapper, new CompetitionMessagingProperties());
  }

  /** Called inside the notice-publish transaction; the broker is never contacted here. */
  @Transactional
  public void enqueueNoticePublished(CompetitionNoticeEntity notice, Long operatorId) {
    LocalDateTime now = LocalDateTime.now();
    CompetitionOutboxEventEntity outboxEvent = new CompetitionOutboxEventEntity();
    outboxEvent.setTenantId(notice.getTenantId());
    outboxEvent.setEventChannel("notice");
    outboxEvent.setAggregateType(NOTICE_AGGREGATE_TYPE);
    outboxEvent.setAggregateId(notice.getId());
    outboxEvent.setEventType(NOTICE_PUBLISHED_EVENT_TYPE);
    outboxEvent.setRoutingKey(NOTICE_PUBLISHED_EVENT_TYPE);
    outboxEvent.setEventKey("notice-published:" + notice.getId());
    outboxEvent.setPayloadJson(serialize(new CompetitionNoticePublishedEvent(
        notice.getTenantId(), notice.getEventId(), notice.getId(), notice.getPublishedAt())));
    outboxEvent.setStatus("pending");
    outboxEvent.setAttemptCount(0);
    outboxEvent.setNextAttemptAt(now);
    outboxEvent.setCreatedBy(operatorId);
    outboxEvent.setUpdatedBy(operatorId);
    outboxEvent.setCreatedAt(now);
    outboxEvent.setUpdatedAt(now);
    outboxEvent.setDeleted(0);
    if (outboxEventMapper.insert(outboxEvent) != 1) {
      log.error("Failed to persist competition notice outbox event, tenantId={}, noticeId={}",
          notice.getTenantId(), notice.getId());
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "公告投递事件创建失败");
    }
    wakeOutboxAfterCommit();
  }

  /** Adds a realtime broadcast to the same DB transaction that inserted these inbox rows. */
  @Transactional
  public void enqueuePlayerMessageAvailable(
      CompetitionPlayerMessageAvailableBatch batch, int batchNumber) {
    if (batch == null || batch.recipients() == null || batch.recipients().isEmpty()) {
      return;
    }
    LocalDateTime now = LocalDateTime.now();
    CompetitionOutboxEventEntity outboxEvent = new CompetitionOutboxEventEntity();
    outboxEvent.setTenantId(batch.tenantId());
    outboxEvent.setEventChannel("notice");
    outboxEvent.setAggregateType("competition_notice_realtime");
    outboxEvent.setAggregateId(batch.noticeId());
    outboxEvent.setEventType(PLAYER_MESSAGE_AVAILABLE_EVENT_TYPE);
    outboxEvent.setRoutingKey(properties.getPlayerRealtimeRoutingKey());
    Long firstMessageId = batch.recipients().get(0).messageId();
    outboxEvent.setEventKey("player-message-available:" + batch.noticeId() + ":"
        + batchNumber + ":" + firstMessageId);
    outboxEvent.setPayloadJson(serialize(batch));
    outboxEvent.setStatus("pending");
    outboxEvent.setAttemptCount(0);
    outboxEvent.setNextAttemptAt(now);
    outboxEvent.setCreatedAt(now);
    outboxEvent.setUpdatedAt(now);
    outboxEvent.setDeleted(0);
    if (outboxEventMapper.insert(outboxEvent) != 1) {
      log.error("Failed to persist competition player realtime outbox event, tenantId={}, noticeId={}, batchNumber={}",
          batch.tenantId(), batch.noticeId(), batchNumber);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "公告实时事件创建失败");
    }
    wakeOutboxAfterCommit();
  }

  private void wakeOutboxAfterCommit() {
    if (outboxWakeup != null) {
      outboxWakeup.wakeAfterCommit();
    }
  }

  private String serialize(CompetitionNoticePublishedEvent payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      log.error("Failed to serialize competition notice outbox payload, tenantId={}, noticeId={}",
          payload.tenantId(), payload.noticeId(), ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "公告投递事件序列化失败");
    }
  }

  private String serialize(CompetitionPlayerMessageAvailableBatch payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      log.error("Failed to serialize competition player realtime outbox payload, tenantId={}, noticeId={}",
          payload.tenantId(), payload.noticeId(), ex);
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "公告实时事件序列化失败");
    }
  }
}
