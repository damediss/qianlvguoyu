package com.guoyu.platform.competition.messaging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.competition.entity.CompetitionNoticeEntity;
import com.guoyu.platform.competition.messaging.entity.CompetitionOutboxEventEntity;
import com.guoyu.platform.competition.messaging.mapper.CompetitionOutboxEventMapper;
import com.guoyu.platform.competition.messaging.model.CompetitionNoticePublishedEvent;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class CompetitionOutboxApplicationService {

  public static final String NOTICE_PUBLISHED_EVENT_TYPE = "competition.notice.published.v1";
  private static final String NOTICE_AGGREGATE_TYPE = "competition_notice";

  private final CompetitionOutboxEventMapper outboxEventMapper;
  private final ObjectMapper objectMapper;

  public CompetitionOutboxApplicationService(
      CompetitionOutboxEventMapper outboxEventMapper,
      ObjectMapper objectMapper) {
    this.outboxEventMapper = outboxEventMapper;
    this.objectMapper = objectMapper;
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
}
